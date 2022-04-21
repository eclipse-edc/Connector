/*
 *  Copyright (c) 2022 Amadeus
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

package org.eclipse.dataspaceconnector.transfer.dataplane.core.proxy;

import org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneProxyTokenGenerator;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyCreationRequest;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyCreator;
import org.jetbrains.annotations.NotNull;

public class DataPlaneTransferProxyCreatorImpl implements DataPlaneTransferProxyCreator {

    private final String endpoint;
    private final DataPlaneProxyTokenGenerator tokenGenerator;

    public DataPlaneTransferProxyCreatorImpl(String endpoint, DataPlaneProxyTokenGenerator tokenGenerator) {
        this.endpoint = endpoint;
        this.tokenGenerator = tokenGenerator;
    }

    /**
     * Creates an {@link EndpointDataReference} targetting public API of the provided Data Plane so that it is used
     * as a proxy to query the data from the data source.
     */
    @Override
    public Result<EndpointDataReference> createProxy(@NotNull DataPlaneTransferProxyCreationRequest request) {
        var tokenGenerationResult = tokenGenerator.generate(request.getAddress(), request.getContractId());
        if (tokenGenerationResult.failed()) {
            return Result.failure(tokenGenerationResult.getFailureMessages());
        }

        var builder = EndpointDataReference.Builder.newInstance()
                .id(request.getId())
                .endpoint(endpoint)
                .authKey(DataPlaneConstants.PUBLIC_API_AUTH_HEADER)
                .authCode(tokenGenerationResult.getContent().getToken())
                .properties(request.getProperties());
        return Result.success(builder.build());
    }
}
