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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.cache.query;

import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.CatalogRequest;

import java.util.concurrent.CompletableFuture;

import static org.eclipse.dataspaceconnector.common.types.Cast.cast;

public class IdsMultipartNodeQueryAdapter implements NodeQueryAdapter {
    public static final String IDS_MULTIPART_PROTOCOL = "ids-multipart";
    private final String connectorId;
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;

    public IdsMultipartNodeQueryAdapter(String connectorId, RemoteMessageDispatcherRegistry dispatcherRegistry) {
        this.connectorId = connectorId;
        this.dispatcherRegistry = dispatcherRegistry;
    }

    @Override
    public CompletableFuture<UpdateResponse> sendRequest(UpdateRequest updateRequest) {
        CatalogRequest catalogRequest = CatalogRequest.Builder.newInstance()
                .protocol(IDS_MULTIPART_PROTOCOL)
                .connectorAddress(getNodeUrl(updateRequest))
                .connectorId(connectorId)
                .build();

        CompletableFuture<Catalog> future = cast(dispatcherRegistry.send(Object.class, catalogRequest, () -> null));

        return future.thenApply(catalog -> new UpdateResponse(getNodeUrl(updateRequest), catalog));
    }

    // adds /api/ids/data if not already there
    private String getNodeUrl(UpdateRequest updateRequest) {
        var url = updateRequest.getNodeUrl();
        if (!url.endsWith("/ids/data")) {
            url += "/api/v1/ids/data";
        }

        return url;
    }
}
