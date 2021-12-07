package org.eclipse.dataspaceconnector.spi.transfer;

import org.eclipse.dataspaceconnector.spi.result.Failure;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;

import java.util.List;

public class ResponseFailure extends Failure {
    private final ResponseStatus status;

    public ResponseFailure(ResponseStatus status, List<String> messages) {
        super(messages);
        this.status = status;
    }

    public ResponseStatus status() {
        return status;
    }
}
