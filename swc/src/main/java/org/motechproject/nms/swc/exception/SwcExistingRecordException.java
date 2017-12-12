package org.motechproject.nms.swc.exception;

/**
 * Signals an issue with importing an FLW which already exits in database.
 */
public class SwcExistingRecordException extends Exception {

    public SwcExistingRecordException(String message) {
        super(message);
    }
}
