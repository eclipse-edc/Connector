/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.dataplane.spi.result;

import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.eclipse.dataspaceconnector.spi.transfer.ResponseFailure;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;

import java.util.List;

/**
 * The result of a transfer operation.
 */
public class TransferResult extends AbstractResult<String, ResponseFailure> {

    public static TransferResult success() {
        return new TransferResult(null, null);
    }

    public static TransferResult failure(ResponseStatus status, String error) {
        return new TransferResult(null, new ResponseFailure(status, List.of(error)));
    }

    protected TransferResult(String content, ResponseFailure failure) {
        super(content, failure);
    }

}
