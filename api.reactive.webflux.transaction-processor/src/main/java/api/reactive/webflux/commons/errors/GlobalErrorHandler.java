package api.reactive.webflux.commons.errors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import api.reactive.webflux.commons.exceptions.ServiceException;
import api.reactive.webflux.commons.exceptions.DuplicateResourceException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@Order(-2)
@Slf4j
@RequiredArgsConstructor
public class GlobalErrorHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    private static final String MSG_INTERNAL_ERROR = "An unexpected internal error has occurred";

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = resolveStatus(ex);

        ApiErrorResponse error = ApiErrorResponse.builder()
                .code(resolveCode(ex))
                .message(resolveMessage(ex))
                .details(resolveDetails(ex))
                .path(exchange.getRequest().getPath().value())
                .timestamp(LocalDateTime.now())
                .build();

        logError(ex, status);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(error);
            return exchange.getResponse().writeWith(Mono.just(
                    exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return HttpStatus.valueOf(rse.getStatusCode().value());
        }
        if (ex instanceof InvalidPathVariableException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ex instanceof ServiceException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ex instanceof DuplicateResourceException) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveCode(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            if (rse.getStatusCode().is4xxClientError()) {
                return ErrorCode.VALIDATION_ERROR.name();
            }
            if (rse.getStatusCode().is5xxServerError()) {
                return ErrorCode.INTERNAL_ERROR.name();
            }
        }
        if (ex instanceof InvalidPathVariableException || ex instanceof ServiceException) {
            return ErrorCode.VALIDATION_ERROR.name();
        }
        if (ex instanceof DuplicateResourceException) {
            return ErrorCode.RESOURCE_ALREADY_EXISTS.name();
        }
        return ErrorCode.INTERNAL_ERROR.name();
    }

    private String resolveMessage(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return rse.getReason();
        }
        if (ex instanceof InvalidPathVariableException || ex instanceof ServiceException) {
            return "Validation error";
        }
        if (ex instanceof DuplicateResourceException) {
            return "The resource already exists";
        }
        return MSG_INTERNAL_ERROR;
    }

    private String resolveDetails(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return rse.getReason();
        }
        return ex.getMessage();
    }

    private void logError(Throwable ex, HttpStatus status) {
        if (status.is5xxServerError()) {
            log.error("Unhandled server error", ex);
        } else {
            log.warn("Handled error [{}]: {}", status, ex.getMessage());
        }
    }
}
