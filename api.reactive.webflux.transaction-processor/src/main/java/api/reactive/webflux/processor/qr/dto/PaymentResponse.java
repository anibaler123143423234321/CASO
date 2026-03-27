package api.reactive.webflux.processor.qr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    @JsonProperty("payment_id")
    private String paymentId;

    private String status;

    private String message;

    private LocalDateTime timestamp;
}
