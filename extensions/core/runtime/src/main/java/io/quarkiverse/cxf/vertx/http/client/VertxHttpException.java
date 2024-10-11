package io.quarkiverse.cxf.vertx.http.client;

public class VertxHttpException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public VertxHttpException(String message, Throwable cause) {
        super(message, cause);
    }

    public VertxHttpException(String message) {
        super(message);
    }

    public VertxHttpException(Throwable cause) {
        super(cause);
    }

}
