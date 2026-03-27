package api.reactive.webflux.processor.qr.mapper;

import api.reactive.webflux.processor.qr.dto.PaymentRequest;
import api.reactive.webflux.processor.qr.dto.PaymentResponse;
import api.reactive.webflux.processor.qr.entity.Transaction;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentMapperImpl implements PaymentMapper {

    @Override
    public Transaction toEntity(PaymentRequest request) {
        if (request == null) {
            return null;
        }
        return Transaction.builder()
                .paymentId(request.getPaymentId())
                .userId(request.getUserId())
                .merchantId(request.getMerchantId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .createdAt(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public PaymentResponse toResponse(Transaction transaction, String message) {
        if (transaction == null) {
            return null;
        }
        return PaymentResponse.builder()
                .paymentId(transaction.getPaymentId())
                .status(transaction.getStatus())
                .message(message)
                .timestamp(LocalDateTime.parse(transaction.getCreatedAt()))
                .build();
    }
}
