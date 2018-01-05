package org.motechproject.wa.swc.exception;

/**
 * Signals an issue with importing an SWC.
 */
public class SwcImportException extends RuntimeException {

    private static final long serialVersionUID = 4526536536032174107L;

    public SwcImportException(String message) {
        super(message);
    }

    public SwcImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
