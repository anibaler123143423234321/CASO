package api.reactive.webflux.processor.qr.mapper;

import api.reactive.webflux.processor.qr.dto.PaymentRequest;
import api.reactive.webflux.processor.qr.dto.PaymentResponse;
import api.reactive.webflux.processor.qr.entity.Transaction;

public interface PaymentMapper {
    Transaction toEntity(PaymentRequest request);
    PaymentResponse toResponse(Transaction transaction, String message);
}
