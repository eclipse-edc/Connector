package org.eclipse.dataspaceconnector.ids.daps;

public class DatServiceException extends Exception {
    public DatServiceException() {
    }

    public DatServiceException(String message) {
        super(message);
    }

    public DatServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatServiceException(Throwable cause) {
        super(cause);
    }

    public DatServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
