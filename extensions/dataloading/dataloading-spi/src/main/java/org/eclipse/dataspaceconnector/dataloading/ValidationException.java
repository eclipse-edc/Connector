package org.eclipse.dataspaceconnector.dataloading;

import org.eclipse.dataspaceconnector.spi.EdcException;

public class ValidationException extends EdcException {

    public ValidationException(String reason) {
        super(reason);
    }
}
