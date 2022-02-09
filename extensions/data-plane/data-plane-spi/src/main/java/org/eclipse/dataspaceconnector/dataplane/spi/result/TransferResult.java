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

import org.eclipse.dataspaceconnector.spi.response.ResponseFailure;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.result.AbstractResult;

import java.util.List;

/**
 * The result of a transfer operation.
 */
public class TransferResult extends AbstractResult<Void, ResponseFailure> {

    public static TransferResult success() {
        return new TransferResult(null);
    }

    public static TransferResult failure(ResponseStatus status, String error) {
        return new TransferResult(new ResponseFailure(status, List.of(error)));
    }

    protected TransferResult(ResponseFailure failure) {
        super(null, failure);
    }
}
