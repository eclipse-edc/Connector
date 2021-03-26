package com.microsoft.dagx.spi.transfer;

/**
 *
 */
public class TransferInitiateResponse {
    public enum Status {OK, ERROR, RETRY}

    private String id;
    private Status status;

    /**
     * The unique process id, which can be used for correlation.
     */
    public String getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }
}
