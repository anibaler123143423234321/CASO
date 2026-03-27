package api.reactive.webflux.processor.qr.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration
public class PaymentRouter {

    @Bean
    public RouterFunction<ServerResponse> paymentRoutes(PaymentHandler handler) {
        return RouterFunctions.route(
                POST("/payment").and(accept(MediaType.APPLICATION_JSON)),
                handler::processPayment
        )
        .andRoute(
                GET("/accounts/{id}").and(accept(MediaType.APPLICATION_JSON)),
                handler::getAccountBalance
        );
    }
}
