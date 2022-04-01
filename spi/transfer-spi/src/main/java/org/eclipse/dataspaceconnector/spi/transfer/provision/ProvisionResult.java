/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.transfer.provision;

import org.eclipse.dataspaceconnector.spi.response.ResponseFailure;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;

import java.util.List;

/**
 * The result of a provision operation.
 */
public class ProvisionResult extends AbstractResult<ProvisionResponse, ResponseFailure> {

    public static ProvisionResult success(ProvisionResponse response) {
        return new ProvisionResult(response, null);
    }

    public static ProvisionResult failure(ResponseStatus status, String error) {
        return new ProvisionResult(null, new ResponseFailure(status, List.of(error)));
    }

    private ProvisionResult(ProvisionResponse response, ResponseFailure failure) {
        super(response, failure);
    }
}
