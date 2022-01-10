/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.transfer.flow;

import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.eclipse.dataspaceconnector.spi.transfer.ResponseFailure;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;

import java.util.List;

/**
 * The result of a data flow request.
 */
public class DataFlowInitiateResult extends AbstractResult<String, ResponseFailure> {

    public static DataFlowInitiateResult success(String content) {
        return new DataFlowInitiateResult(content, null);
    }

    public static DataFlowInitiateResult failure(ResponseStatus status, String error) {
        return new DataFlowInitiateResult(null, new ResponseFailure(status, List.of(error)));
    }

    public DataFlowInitiateResult(String content) {
        super(content, null);
    }

    public DataFlowInitiateResult(ResponseStatus status, String error) {
        super(null, new ResponseFailure(status, List.of(error)));
    }

    private DataFlowInitiateResult(String content, ResponseFailure failure) {
        super(content, failure);
    }

}
