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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IdsMultipartNodeQueryAdapter implements NodeQueryAdapter {
    public static final String IDS_MULTIPART_PROTOCOL = "ids-multipart";
    private final String connectorId;
    private final BatchedRequestFetcher requestFetcher;

    public IdsMultipartNodeQueryAdapter(String connectorId, RemoteMessageDispatcherRegistry dispatcherRegistry) {
        this.connectorId = connectorId;
        requestFetcher = new BatchedRequestFetcher(dispatcherRegistry);
    }

    @Override
    public CompletableFuture<UpdateResponse> sendRequest(UpdateRequest updateRequest) {

        var catalogRequest = CatalogRequest.Builder.newInstance()
                .protocol(IDS_MULTIPART_PROTOCOL)
                .connectorAddress(getNodeUrl(updateRequest))
                .connectorId(connectorId)
                .build();
        var allOffers = requestFetcher.fetch(catalogRequest, 0, 100);

        return allOffers.thenApply(list -> new UpdateResponse(getNodeUrl(updateRequest), Catalog.Builder.newInstance().id(UUID.randomUUID().toString()).contractOffers(list).build()));
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
