package org.eclipse.dataspaceconnector.ids.core.configuration;

import org.eclipse.dataspaceconnector.spi.EdcException;

public class MissingConfigurationException extends EdcException {

    public MissingConfigurationException(String message) {
        super(message);
    }

    public MissingConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingConfigurationException(Throwable cause) {
        super(cause);
    }

    public MissingConfigurationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
