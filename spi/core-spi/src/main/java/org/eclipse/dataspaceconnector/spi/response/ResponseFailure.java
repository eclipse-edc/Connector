package org.eclipse.dataspaceconnector.spi.response;

import org.eclipse.dataspaceconnector.spi.result.Failure;

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
