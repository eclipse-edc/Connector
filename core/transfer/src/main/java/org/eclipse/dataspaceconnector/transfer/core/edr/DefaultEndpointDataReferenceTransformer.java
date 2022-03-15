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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.edr;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

/**
 * Default implementation of an {@link EndpointDataReferenceTransformer} which returns the input EDR
 * as it comes without any change.
 */
public class DefaultEndpointDataReferenceTransformer implements EndpointDataReferenceTransformer {

    @Override
    public Result<EndpointDataReference> transform(@NotNull EndpointDataReference edr) {
        return Result.success(edr);
    }
}
