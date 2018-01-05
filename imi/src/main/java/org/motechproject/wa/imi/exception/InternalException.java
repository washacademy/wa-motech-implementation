package org.motechproject.wa.imi.exception;

public class InternalException extends IllegalStateException {
    public InternalException(String message) {
        super(message);
    }

    public InternalException(String message, Throwable cause) {
        super(message, cause);
    }
}
