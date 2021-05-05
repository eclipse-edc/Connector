/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.transfer.flow;

import com.microsoft.dagx.spi.transfer.response.ResponseStatus;

/**
 * The result of a data flow request.
 */
public class DataFlowInitiateResponse {
    public static DataFlowInitiateResponse OK = new DataFlowInitiateResponse();

    private ResponseStatus status;
    private String error;

    private DataFlowInitiateResponse() {
        this.status = ResponseStatus.OK;
    }

    public DataFlowInitiateResponse(ResponseStatus status, String error) {
        this.status = status;
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public ResponseStatus getStatus() {
        return status;
    }
}
