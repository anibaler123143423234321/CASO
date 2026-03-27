package api.reactive.webflux.commons.exceptions;

import lombok.Getter;

@Getter
public class DuplicateResourceException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String field;
    private final String value;

    public DuplicateResourceException(String field, String value) {
        super(String.format("El valor '%s' ya existe para el campo '%s'", value, field));
        this.field = field;
        this.value = value;
    }
}
