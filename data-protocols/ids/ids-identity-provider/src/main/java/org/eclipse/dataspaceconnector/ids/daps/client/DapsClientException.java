package org.eclipse.dataspaceconnector.ids.daps.client;

public class DapsClientException extends RuntimeException {

    public DapsClientException(final String message) {
        super(message);
    }

    public DapsClientException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
