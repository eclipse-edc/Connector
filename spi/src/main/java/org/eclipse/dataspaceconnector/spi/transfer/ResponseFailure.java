package org.eclipse.dataspaceconnector.spi.transfer;

import org.eclipse.dataspaceconnector.spi.result.Failure;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;

import java.util.List;

public class ResponseFailure implements Failure {
    private final ResponseStatus status;
    private final List<String> messages;

    public ResponseFailure(ResponseStatus status, List<String> messages) {
        this.status = status;
        this.messages = messages;
    }

    @Override
    public List<String> getMessages() {
        return messages;
    }

    public ResponseStatus status() {
        return status;
    }
}
