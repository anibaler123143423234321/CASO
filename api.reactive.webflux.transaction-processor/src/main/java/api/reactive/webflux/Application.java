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
				String body = (String) map.get("body");
				if (body == null) {
					return Mono.just(PaymentResponse.builder()
							.status("ERROR")
							.message("Body is missing")
							.timestamp(java.time.LocalDateTime.now())
							.build());
				}
				PaymentRequest request = mapper.readValue(body, PaymentRequest.class);
				return service.processPayment(request);
			} catch (Exception e) {
				log.error("Error processing payment: {}", e.getMessage());
				return Mono.just(PaymentResponse.builder()
						.status("ERROR")
						.message("Error: " + e.getMessage())
						.timestamp(java.time.LocalDateTime.now())
						.build());
			}
		};
	}

	@Bean(name = "getAccountBalance")
	public Function<Map<String, Object>, Mono<api.reactive.webflux.dto.AccountBalanceResponse>> getAccountBalance(PaymentService service) {
		return map -> {
			try {
				// Extraer ID de pathParameters de API Gateway
				Map<String, String> pathParams = (Map<String, String>) map.get("pathParameters");
				String id = pathParams != null ? pathParams.get("id") : null;
				
				if (id == null) {
					// Fallback a query string si no está en path
					Map<String, String> queryParams = (Map<String, String>) map.get("queryStringParameters");
					id = queryParams != null ? queryParams.get("id") : "USER-001";
				}
				
				return service.getAccountBalance(id);
			} catch (Exception e) {
				log.error("Error fetching balance in function: {}", e.getMessage());
				return Mono.empty();
			}
		};
	}

}
