package com.microsoft.dagx.spi.transfer;

import com.microsoft.dagx.spi.transfer.response.ResponseStatus;

/**
 *
 */
public class TransferInitiateResponse {
    private String id;
    private ResponseStatus status;

    /**
     * The unique process id, which can be used for correlation.
     */
    public String getId() {
        return id;
    }

    public ResponseStatus getStatus() {
        return status;
    }
}
