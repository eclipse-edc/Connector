package com.microsoft.dagx.spi.transfer;

/**
 * The result of a transfer request.
 */
public class TransferResponse {
    private Status status;

    public enum Status {OK, ERROR}

    public TransferResponse(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
