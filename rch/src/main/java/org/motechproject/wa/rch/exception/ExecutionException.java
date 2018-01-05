package org.motechproject.wa.rch.exception;

public class ExecutionException extends RuntimeException {

    public ExecutionException(String message) {
        super(message);
    }

    public ExecutionException(String message, Throwable t) {
        super(message, t);
    }
}
