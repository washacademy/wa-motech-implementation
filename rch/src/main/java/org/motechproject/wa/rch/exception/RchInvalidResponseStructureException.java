package org.motechproject.wa.rch.exception;

public class RchInvalidResponseStructureException extends RuntimeException {

    public RchInvalidResponseStructureException(String message) {
        super(message);
    }

    public RchInvalidResponseStructureException(String message, Throwable t) {
        super(message, t);
    }
}
