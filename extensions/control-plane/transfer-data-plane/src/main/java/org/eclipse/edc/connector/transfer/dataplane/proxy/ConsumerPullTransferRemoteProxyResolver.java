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

package org.eclipse.edc.connector.transfer.dataplane.proxy;

import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;

public class ConsumerPullTransferRemoteProxyResolver implements ConsumerPullTransferProxyResolver {
    private static final String PUBLIC_API_URL_PROPERTY = "publicApiUrl";

    private final DataPlaneSelectorClient selectorClient;
    private final String selectorStrategy;


    public ConsumerPullTransferRemoteProxyResolver(DataPlaneSelectorClient selectorClient, String selectorStrategy) {
        this.selectorClient = selectorClient;
        this.selectorStrategy = selectorStrategy;
    }

    @Override
    public Result<String> resolveProxyUrl(DataAddress source) {
        var dataPlaneInstance = selectorClient.find(source, destinationAddress(), selectorStrategy);
        if (dataPlaneInstance == null) {
            return Result.failure("Failed to find DataPlaneInstance for proxying data from source: " + source.getType());
        }
        var publicApiUrl = dataPlaneInstance.getProperties().get(PUBLIC_API_URL_PROPERTY);
        if (publicApiUrl == null) {
            return Result.failure(String.format("Missing property `%s` in DataPlaneInstance", PUBLIC_API_URL_PROPERTY));
        }
        return Result.success(publicApiUrl.toString());
    }


    private static DataAddress destinationAddress() {
        return DataAddress.Builder.newInstance().type(HTTP_PROXY).build();
    }
}
