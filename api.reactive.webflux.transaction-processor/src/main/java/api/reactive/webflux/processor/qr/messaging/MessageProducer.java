package api.reactive.webflux.processor.qr.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProducer {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;

    @Value("${app.sqs.queue-url:}")
    private String queueUrl;

    /**
     * Envía un evento de transacción exitosa a la cola SQS
     */
    public Mono<Void> sendTransactionEvent(String paymentId, String userId, String merchantId, String amount, String status) {
        if (queueUrl == null || queueUrl.isEmpty()) {
            log.warn("SQS queue URL not configured, skipping message for paymentId={}", paymentId);
            return Mono.empty();
        }

        return Mono.fromCallable(() -> {
            Map<String, String> event = Map.of(
                    "paymentId", paymentId,
                    "userId", userId,
                    "merchantId", merchantId,
                    "amount", amount,
                    "status", status,
                    "timestamp", java.time.Instant.now().toString()
            );
            return objectMapper.writeValueAsString(event);
        }).flatMap(messageBody -> {
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();
            log.info("Enviando evento de transacción a SQS: paymentId={}", paymentId);
            return Mono.fromFuture(sqsAsyncClient.sendMessage(request));
        }).doOnSuccess(response -> log.info("Mensaje enviado a SQS: messageId={}", response != null ? response.messageId() : "N/A"))
          .doOnError(ex -> log.error("Error enviando mensaje a SQS: {}", ex.getMessage()))
          .then();
    }
}
