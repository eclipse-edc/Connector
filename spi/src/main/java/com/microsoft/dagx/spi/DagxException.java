package com.microsoft.dagx.spi;

/**
 * Base exception for the system.
 *
 * The system should use unchecked exceptions when appropriate (e.g., non-recoverable errors) and may extend this exception.
 */
public class DagxException extends RuntimeException {

    public DagxException(String message) {
        super(message);
    }

    public DagxException(String message, Throwable cause) {
        super(message, cause);
    }

    public DagxException(Throwable cause) {
        super(cause);
    }

    public DagxException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
