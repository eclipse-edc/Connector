package com.microsoft.dagx.spi.transfer.flow;

/**
 * The result of a data flow request.
 */
public class DataFlowInitiateResponse {
    public static DataFlowInitiateResponse OK = new DataFlowInitiateResponse();

    private Status status;
    private String error;

    public enum Status {OK, ERROR_RETRY, FATAL_ERROR}

    private DataFlowInitiateResponse() {
        this.status = Status.OK;
    }

    public DataFlowInitiateResponse(Status status, String error) {
        this.status = status;
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public Status getStatus() {
        return status;
    }
}
