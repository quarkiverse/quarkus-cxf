package org.apache.axiom.om;

public class OMException extends RuntimeException {

    /**  */
    private static final long serialVersionUID = 1L;

    public OMException() {
    }

    public OMException(String message) {
        super(message);
    }

    public OMException(String message, Throwable cause) {
        super(message, cause);
    }

    public OMException(Throwable cause) {
        super(cause);
    }

}
