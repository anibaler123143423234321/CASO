package api.reactive.webflux.processor.qr.service;

import api.reactive.webflux.processor.qr.dto.AccountBalanceResponse;
import api.reactive.webflux.processor.qr.dto.PaymentRequest;
import api.reactive.webflux.processor.qr.dto.PaymentResponse;
import reactor.core.publisher.Mono;

public interface PaymentService {
    Mono<PaymentResponse> processPayment(PaymentRequest request);
    Mono<AccountBalanceResponse> getAccountBalance(String id);
}
