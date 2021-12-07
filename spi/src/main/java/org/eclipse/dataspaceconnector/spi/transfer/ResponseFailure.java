package org.eclipse.dataspaceconnector.spi.transfer;

import org.eclipse.dataspaceconnector.spi.result.Failure;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;

import java.util.List;

public class ResponseFailure implements Failure {
    @Override
    public List<String> getMessages() {
        return null;
    }

    public ResponseStatus status() {
        return null;
    }
}
