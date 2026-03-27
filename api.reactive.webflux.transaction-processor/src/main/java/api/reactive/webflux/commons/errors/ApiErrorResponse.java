package api.reactive.webflux.commons.errors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    private String code;
    private String message;
    private String details;
    private String path;
    private LocalDateTime timestamp;
}
