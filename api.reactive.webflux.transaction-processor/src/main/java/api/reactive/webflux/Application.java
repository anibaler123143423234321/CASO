package api.reactive.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;
import java.util.function.Function;
import java.util.Map;
import api.reactive.webflux.dto.PaymentRequest;
import api.reactive.webflux.dto.PaymentResponse;
import api.reactive.webflux.service.PaymentService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean(name = "handlePayment")
	public Function<Map<String, Object>, Mono<PaymentResponse>> handlePayment(PaymentService service, com.fasterxml.jackson.databind.ObjectMapper mapper) {
		return map -> {
			try {
				PaymentRequest request;

				// El HTTP API Gateway v2 envuelve el body en un evento completo
				if (map.containsKey("body")) {
					String body = (String) map.get("body");
					log.info("Extracting body from HTTP event: {}", body);
					request = mapper.readValue(body, PaymentRequest.class);
				} else {
					// Llamada directa (sin API Gateway)
					request = mapper.convertValue(map, PaymentRequest.class);
				}

				log.info("Deserialized request: userId={}, paymentId={}, amount={}", 
						request.getUserId(), request.getPaymentId(), request.getAmount());
				return service.processPayment(request);
			} catch (Exception e) {
				log.error("Error deserializing request: {}", e.getMessage(), e);
				return Mono.just(new PaymentResponse(
						null,
						"ERROR",
						"Error deserializing request: " + e.getMessage(),
						java.time.LocalDateTime.now()
				));
			}
		};
	}

}
