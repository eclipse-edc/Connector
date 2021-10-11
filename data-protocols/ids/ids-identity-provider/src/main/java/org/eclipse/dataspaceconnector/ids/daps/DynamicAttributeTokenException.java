package org.eclipse.dataspaceconnector.ids.daps;

public class DynamicAttributeTokenException extends Exception {
    public DynamicAttributeTokenException() {
    }

    public DynamicAttributeTokenException(String message) {
        super(message);
    }

    public DynamicAttributeTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public DynamicAttributeTokenException(Throwable cause) {
        super(cause);
    }

    public DynamicAttributeTokenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
