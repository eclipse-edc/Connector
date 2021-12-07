package org.eclipse.dataspaceconnector.spi.contract.negotiation.response;

import org.eclipse.dataspaceconnector.spi.result.Failure;

import java.util.Collections;
import java.util.List;

public class StatusFailure implements Failure {
    private final NegotiationResult.Status status;

    public StatusFailure(NegotiationResult.Status status) {
        this.status = status;
    }

    @Override
    public List<String> getMessages() {
        return Collections.emptyList();
    }

    public NegotiationResult.Status getStatus() {
        return status;
    }
}
