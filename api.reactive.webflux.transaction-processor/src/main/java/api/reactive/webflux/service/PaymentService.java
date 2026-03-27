package api.reactive.webflux.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import api.reactive.webflux.entity.MerchantAccount;
import api.reactive.webflux.entity.Transaction;
import api.reactive.webflux.entity.UserAccount;
import api.reactive.webflux.dto.PaymentRequest;
import api.reactive.webflux.dto.PaymentResponse;
import api.reactive.webflux.messaging.MessageProducer;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class PaymentService {

    private final DynamoDbAsyncTable<UserAccount> userTable;
    private final DynamoDbAsyncTable<MerchantAccount> merchantTable;
    private final DynamoDbAsyncTable<Transaction> transactionTable;
    private final MessageProducer messageProducer;
    private final Cache transactionCache;

    public PaymentService(DynamoDbEnhancedAsyncClient enhancedClient, MessageProducer messageProducer, CacheManager cacheManager) {
        this.userTable = enhancedClient.table("user-accounts-dev", TableSchema.fromBean(UserAccount.class));
        this.merchantTable = enhancedClient.table("merchant-accounts-dev", TableSchema.fromBean(MerchantAccount.class));
        this.transactionTable = enhancedClient.table("transaction-processor-table-dev", TableSchema.fromBean(Transaction.class));
        this.messageProducer = messageProducer;
        this.transactionCache = cacheManager.getCache("transactions");
    }

    /**
     * Procesa un pago con:
     * - Caching: verifica cache antes de DynamoDB
     * - Idempotencia: verifica paymentId existente en DynamoDB
     * - Resiliencia: CircuitBreaker + TimeLimiter + Retry via Resilience4j
     * - Mensajería: publica evento a SQS tras pago exitoso
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @TimeLimiter(name = "paymentService")
    @Retry(name = "paymentService")
    public Mono<PaymentResponse> processPayment(PaymentRequest request) {
        log.info("Procesando pago: paymentId={}, userId={}, amount={}",
                request.getPaymentId(), request.getUserId(), request.getAmount());

        // [1] CACHE CHECK: verificar si ya procesamos este pago
        if (transactionCache != null) {
            PaymentResponse cached = transactionCache.get(request.getPaymentId(), PaymentResponse.class);
            if (cached != null) {
                log.info("CACHE HIT: paymentId={} encontrado en cache", request.getPaymentId());
                return Mono.just(cached);
            }
        }

        // [2] DYNAMODB CHECK: verificar idempotencia en la base de datos
        Key txKey = Key.builder().partitionValue(request.getPaymentId()).build();

        return Mono.fromFuture(transactionTable.getItem(txKey))
                .flatMap(existingTx -> {
                    log.info("Transacción idempotente detectada en DynamoDB: paymentId={}", request.getPaymentId());
                    PaymentResponse response = toResponse(existingTx, "Transacción ya procesada anteriormente (idempotente)");
                    cacheResponse(request.getPaymentId(), response);
                    return Mono.just(response);
                })
                .switchIfEmpty(executePayment(request));
    }

    private Mono<PaymentResponse> executePayment(PaymentRequest request) {
        Key userKey = Key.builder().partitionValue(request.getUserId()).build();

        return Mono.fromFuture(userTable.getItem(userKey))
                .switchIfEmpty(Mono.defer(() -> saveTransaction(request, Transaction.STATUS_FAILED,
                        "Usuario no encontrado: " + request.getUserId())
                        .flatMap(tx -> Mono.error(new PaymentException(toResponse(tx, tx.getMessage()))))))
                .flatMap(user -> {
                    Key merchantKey = Key.builder().partitionValue(request.getMerchantId()).build();
                    return Mono.fromFuture(merchantTable.getItem(merchantKey))
                            .switchIfEmpty(Mono.defer(() -> saveTransaction(request, Transaction.STATUS_FAILED,
                                    "Comercio no encontrado: " + request.getMerchantId())
                                    .flatMap(tx -> Mono.error(new PaymentException(toResponse(tx, tx.getMessage()))))))
                            .flatMap(merchant -> processTransfer(user, merchant, request));
                })
                .onErrorResume(PaymentException.class, ex -> Mono.just(ex.getResponse()));
    }

    private Mono<PaymentResponse> processTransfer(UserAccount user,
            MerchantAccount merchant,
            PaymentRequest request) {
        BigDecimal amount = request.getAmount();

        // Validar saldo suficiente
        if (user.getBalance().compareTo(amount) < 0) {
            return saveTransaction(request, Transaction.STATUS_INSUFFICIENT_FUNDS,
                    "Saldo insuficiente. Saldo actual: " + user.getBalance() + ", monto solicitado: " + amount)
                    .map(tx -> toResponse(tx, tx.getMessage()));
        }

        // Descontar saldo al usuario
        user.setBalance(user.getBalance().subtract(amount));

        // Acreditar al comercio
        merchant.setBalance(merchant.getBalance().add(amount));

        return Mono.fromFuture(userTable.putItem(user))
                .then(Mono.fromFuture(merchantTable.putItem(merchant)))
                .then(saveTransaction(request, Transaction.STATUS_SUCCESS, "Pago procesado exitosamente"))
                .map(tx -> {
                    PaymentResponse response = toResponse(tx, "Pago procesado exitosamente");

                    // [3] CACHE: guardar resultado en cache
                    cacheResponse(request.getPaymentId(), response);

                    return response;
                })
                // [4] SQS: publicar evento de transacción exitosa (fire-and-forget)
                .flatMap(response -> messageProducer.sendTransactionEvent(
                        request.getPaymentId(),
                        request.getUserId(),
                        request.getMerchantId(),
                        request.getAmount().toString(),
                        Transaction.STATUS_SUCCESS
                ).thenReturn(response));
    }

    private void cacheResponse(String paymentId, PaymentResponse response) {
        if (transactionCache != null) {
            transactionCache.put(paymentId, response);
            log.info("CACHE PUT: paymentId={}", paymentId);
        }
    }

    private Mono<Transaction> saveTransaction(PaymentRequest request, String status, String message) {
        Transaction tx = Transaction.builder()
                .paymentId(request.getPaymentId())
                .userId(request.getUserId())
                .merchantId(request.getMerchantId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(status)
                .message(message)
                .createdAt(LocalDateTime.now().toString())
                .build();
        return Mono.fromFuture(transactionTable.putItem(tx)).thenReturn(tx);
    }

    private PaymentResponse toResponse(Transaction tx, String message) {
        return PaymentResponse.builder()
                .paymentId(tx.getPaymentId())
                .status(tx.getStatus())
                .message(message)
                .timestamp(LocalDateTime.parse(tx.getCreatedAt()))
                .build();
    }

    // =====================================================
    // Fallback para CircuitBreaker
    // =====================================================
    public Mono<PaymentResponse> paymentFallback(PaymentRequest request, Throwable ex) {
        log.error("CircuitBreaker OPEN / Fallback activado para paymentId={}: {}",
                request.getPaymentId(), ex.getMessage());
        return Mono.just(PaymentResponse.builder()
                .paymentId(request.getPaymentId())
                .status("SERVICE_UNAVAILABLE")
                .message("Servicio temporalmente no disponible. Intente nuevamente más tarde.")
                .timestamp(LocalDateTime.now())
                .build());
    }

    // Excepción interna para transportar respuestas de error en el flujo reactivo
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
