package api.reactive.webflux.commons.errors;

public class InvalidPathVariableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidPathVariableException(String message) {
        super(message);
    }
}
