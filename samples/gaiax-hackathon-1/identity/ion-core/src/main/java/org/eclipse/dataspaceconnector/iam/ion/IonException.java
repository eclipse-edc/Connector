package org.eclipse.dataspaceconnector.iam.ion;

import org.eclipse.dataspaceconnector.spi.EdcException;

public class IonException extends EdcException {
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
