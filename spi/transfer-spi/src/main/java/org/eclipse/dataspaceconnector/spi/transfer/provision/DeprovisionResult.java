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
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;

import java.util.List;

/**
 * The result of a deprovision operation.
 */
public class DeprovisionResult extends AbstractResult<DeprovisionedResource, ResponseFailure> {

    public static DeprovisionResult success(DeprovisionedResource resource) {
        return new DeprovisionResult(resource, null);
    }

    public static DeprovisionResult failure(ResponseStatus status, String error) {
        return new DeprovisionResult(null, new ResponseFailure(status, List.of(error)));
    }

    private DeprovisionResult(DeprovisionedResource response, ResponseFailure failure) {
        super(response, failure);
    }
}
