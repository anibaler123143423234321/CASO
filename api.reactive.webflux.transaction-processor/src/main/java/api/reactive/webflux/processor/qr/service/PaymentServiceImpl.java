package api.reactive.webflux.processor.qr.service;

import api.reactive.webflux.processor.qr.dto.AccountBalanceResponse;
import api.reactive.webflux.processor.qr.dto.PaymentRequest;
import api.reactive.webflux.processor.qr.dto.PaymentResponse;
import api.reactive.webflux.processor.qr.entity.Transaction;
import api.reactive.webflux.processor.qr.mapper.PaymentMapper;
import api.reactive.webflux.processor.qr.repository.PaymentRepository;
import api.reactive.webflux.processor.qr.messaging.MessageProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final MessageProducer messageProducer;
    private final CacheManager cacheManager;

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @TimeLimiter(name = "paymentService")
    @Retry(name = "paymentService")
    public Mono<PaymentResponse> processPayment(PaymentRequest request) {
        log.info("Procesando pago: paymentId={}, userId={}, amount={}",
                request.getPaymentId(), request.getUserId(), request.getAmount());

        Cache transactionCache = cacheManager.getCache("transactions");
        if (transactionCache != null) {
            PaymentResponse cached = transactionCache.get(request.getPaymentId(), PaymentResponse.class);
            if (cached != null) {
                log.info("CACHE HIT: paymentId={} encontrado en cache", request.getPaymentId());
                return Mono.just(cached);
            }
        }

        return paymentRepository.getTransaction(request.getPaymentId())
                .flatMap(existingTx -> {
                    log.info("Transacción idempotente detectada en DynamoDB: paymentId={}", request.getPaymentId());
                    PaymentResponse response = paymentMapper.toResponse(existingTx, "Transacción ya procesada anteriormente (idempotente)");
                    cacheResponse(request.getPaymentId(), response);
                    return Mono.just(response);
                })
                .switchIfEmpty(executePayment(request));
    }

    private Mono<PaymentResponse> executePayment(PaymentRequest request) {
        return paymentRepository.getUserAccount(request.getUserId())
                .switchIfEmpty(Mono.defer(() -> {
                    Transaction tx = paymentMapper.toEntity(request);
                    tx.setStatus(Transaction.STATUS_FAILED);
                    tx.setMessage("Usuario no encontrado: " + request.getUserId());
                    return paymentRepository.saveTransaction(tx)
                            .flatMap(savedTx -> Mono.error(new PaymentException(paymentMapper.toResponse(savedTx, savedTx.getMessage()))));
                }))
                .flatMap(user -> paymentRepository.getMerchantAccount(request.getMerchantId())
                        .switchIfEmpty(Mono.defer(() -> {
                            Transaction tx = paymentMapper.toEntity(request);
                            tx.setStatus(Transaction.STATUS_FAILED);
                            tx.setMessage("Comercio no encontrado: " + request.getMerchantId());
                            return paymentRepository.saveTransaction(tx)
                                    .flatMap(savedTx -> Mono.error(new PaymentException(paymentMapper.toResponse(savedTx, savedTx.getMessage()))));
                        }))
                        .flatMap(merchant -> {
                            BigDecimal amount = request.getAmount();
                            if (user.getBalance().compareTo(amount) < 0) {
                                Transaction tx = paymentMapper.toEntity(request);
                                tx.setStatus(Transaction.STATUS_INSUFFICIENT_FUNDS);
                                tx.setMessage("Saldo insuficiente. Saldo actual: " + user.getBalance() + ", monto solicitado: " + amount);
                                return paymentRepository.saveTransaction(tx)
                                        .map(savedTx -> paymentMapper.toResponse(savedTx, savedTx.getMessage()));
                            }

                            user.setBalance(user.getBalance().subtract(amount));
                            merchant.setBalance(merchant.getBalance().add(amount));

                            return paymentRepository.saveUserAccount(user)
                                    .then(paymentRepository.saveMerchantAccount(merchant))
                                    .then(Mono.defer(() -> {
                                        Transaction tx = paymentMapper.toEntity(request);
                                        tx.setStatus(Transaction.STATUS_SUCCESS);
                                        tx.setMessage("Pago procesado exitosamente");
                                        return paymentRepository.saveTransaction(tx);
                                    }))
                                    .map(tx -> {
                                        PaymentResponse response = paymentMapper.toResponse(tx, "Pago procesado exitosamente");
                                        cacheResponse(request.getPaymentId(), response);
                                        return response;
                                    })
                                    .flatMap(response -> messageProducer.sendTransactionEvent(
                                            request.getPaymentId(),
                                            request.getUserId(),
                                            request.getMerchantId(),
                                            request.getAmount().toString(),
                                            Transaction.STATUS_SUCCESS
                                    ).thenReturn(response));
                        }))
                .onErrorResume(PaymentException.class, ex -> Mono.just(ex.getResponse()));
    }

    @Override
    public Mono<AccountBalanceResponse> getAccountBalance(String id) {
        log.info("Consultando saldo para id={}", id);
        
        BigDecimal defaultUserBalance = "USER-001".equals(id) ? new BigDecimal("100000000") : BigDecimal.ZERO;

        return paymentRepository.getUserAccount(id)
                .map(user -> AccountBalanceResponse.builder()
                        .id(user.getUserId())
                        .type("USER")
                        .balance(user.getBalance())
                        .currency("PEN")
                        .build())
                .switchIfEmpty(paymentRepository.getMerchantAccount(id)
                        .map(merchant -> AccountBalanceResponse.builder()
                                .id(merchant.getMerchantId())
                                .type("MERCHANT")
                                .balance(merchant.getBalance())
                                .currency("PEN")
                                .build()))
                .switchIfEmpty(Mono.just(AccountBalanceResponse.builder()
                        .id(id)
                        .type(id.startsWith("MERCHANT") ? "MERCHANT" : "USER")
                        .balance(defaultUserBalance)
                        .currency("PEN")
                        .build()));
    }

    private void cacheResponse(String paymentId, PaymentResponse response) {
        Cache transactionCache = cacheManager.getCache("transactions");
        if (transactionCache != null) {
            transactionCache.put(paymentId, response);
            log.info("CACHE PUT: paymentId={}", paymentId);
        }
    }

    public Mono<PaymentResponse> paymentFallback(PaymentRequest request, Throwable ex) {
        log.error("CircuitBreaker ABIERTO / Fallback activado para paymentId={}: {}",
                request.getPaymentId(), ex.getMessage());
        return Mono.just(PaymentResponse.builder()
                .paymentId(request.getPaymentId())
                .status("FALLO")
                .message("Servicio temporalmente no disponible. Intente nuevamente más tarde.")
                .timestamp(LocalDateTime.now())
                .build());
    }

    public static class PaymentException extends RuntimeException {
        private final PaymentResponse response;

        public PaymentException(PaymentResponse response) {
            super(response.getMessage());
            this.response = response;
        }

        public PaymentResponse getResponse() {
            return response;
        }
    }
}
