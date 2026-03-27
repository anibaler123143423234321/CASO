package api.reactive.webflux.processor.qr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceResponse {
    private String id;
    private String type; // "USER" or "MERCHANT"
    private BigDecimal balance;
    private String currency;
}
