package com.microsoft.dagx.spi.transfer;

import com.microsoft.dagx.spi.transfer.response.ResponseStatus;

/**
 *
 */
public class TransferInitiateResponse {
    private String id;
    private String error;
    private ResponseStatus status = ResponseStatus.OK;

    /**
     * The unique process id, which can be used for correlation.
     */
    public String getId() {
        return id;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    private TransferInitiateResponse() {
    }

    public static class Builder {
        private final TransferInitiateResponse response;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            response.id = id;
            return this;
        }

        public Builder status(ResponseStatus status) {
            response.status = status;
            return this;
        }

        public Builder error(String error) {
            response.error = error;
            return this;
        }

        public TransferInitiateResponse build() {
            return response;
        }

        private Builder() {
            response = new TransferInitiateResponse();
        }
    }



}
