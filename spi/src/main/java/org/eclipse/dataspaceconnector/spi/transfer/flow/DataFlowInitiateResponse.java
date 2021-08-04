/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.transfer.flow;

import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;

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
