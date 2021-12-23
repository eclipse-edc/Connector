package org.eclipse.dataspaceconnector.spi.contract.negotiation.response;

import org.eclipse.dataspaceconnector.spi.result.Failure;

import static java.util.Collections.emptyList;

public class StatusFailure extends Failure {
    private final NegotiationResult.Status status;

    public StatusFailure(NegotiationResult.Status status) {
        super(emptyList());
        this.status = status;
    }

    public NegotiationResult.Status getStatus() {
        return status;
    }
}
