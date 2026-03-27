package api.reactive.webflux.api.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import api.reactive.webflux.entity.Transaction;
import api.reactive.webflux.dto.PaymentRequest;
import api.reactive.webflux.dto.PaymentResponse;
import api.reactive.webflux.service.PaymentService;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentHandler {

    private final PaymentService paymentService;
    private final Validator validator;

    public Mono<ServerResponse> processPayment(ServerRequest request) {
        return request.bodyToMono(PaymentRequest.class)
                .flatMap(this::validate)
                .flatMap(paymentService::processPayment)
                .flatMap(response -> {
                    HttpStatus status = Transaction.STATUS_SUCCESS.equals(response.getStatus())
                            ? HttpStatus.OK
                            : HttpStatus.BAD_REQUEST;

                    // SERVICE_UNAVAILABLE from fallback
                    if ("SERVICE_UNAVAILABLE".equals(response.getStatus())) {
                        status = HttpStatus.SERVICE_UNAVAILABLE;
                    }

                    return ServerResponse.status(status)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(IllegalArgumentException.class, ex -> ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(PaymentResponse.builder()
                                .status("VALIDATION_ERROR")
                                .message(ex.getMessage())
                                .timestamp(LocalDateTime.now())
                                .build()))
                .onErrorResume(ex -> {
                    log.error("Error inesperado procesando pago", ex);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(PaymentResponse.builder()
                                    .status("ERROR")
                                    .message("Error interno del servidor")
                                    .timestamp(LocalDateTime.now())
                                    .build());
                });
    }

    private Mono<PaymentRequest> validate(PaymentRequest request) {
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            return Mono.error(new IllegalArgumentException(errors));
        }
        return Mono.just(request);
    }
}
