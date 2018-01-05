package org.motechproject.wa.imi.exception;

public class ChunkingException extends RuntimeException {
    public ChunkingException(String m, Throwable t) {
        super(m, t);
    }
}
