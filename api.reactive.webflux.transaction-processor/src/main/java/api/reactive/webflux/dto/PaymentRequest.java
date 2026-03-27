package api.reactive.webflux.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "user_id es obligatorio")
    @JsonProperty("user_id")
    private String userId;

    @NotNull(message = "amount es obligatorio")
    @DecimalMin(value = "0.01", message = "amount debe ser mayor a 0")
    private BigDecimal amount;

    @NotBlank(message = "currency es obligatorio")
    private String currency;

    @NotBlank(message = "merchant_id es obligatorio")
    @JsonProperty("merchant_id")
    private String merchantId;

    @NotBlank(message = "payment_id es obligatorio")
    @JsonProperty("payment_id")
    private String paymentId;
}
