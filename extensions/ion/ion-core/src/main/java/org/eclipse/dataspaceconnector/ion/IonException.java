package org.eclipse.dataspaceconnector.ion;

/**
 * Any other ION-related error occurred.
 */
public class IonException extends RuntimeException {
    public IonException(String message) {
        super(message);
    }

    public IonException(String message, Throwable cause) {
        super(message, cause);
    }

    public IonException(Throwable cause) {
        super(cause);
    }

    public IonException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
