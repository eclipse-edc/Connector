/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft - initial API and implementation
 *       ZF Friedrichshafen AG - enable asset filtering
 *
 */

package org.eclipse.edc.connector.controlplane.services.catalog;

import org.eclipse.edc.connector.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.catalog.spi.DatasetRequestMessage;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.concurrent.CompletableFuture;

public class CatalogServiceImpl implements CatalogService {

    private final RemoteMessageDispatcherRegistry dispatcher;

    public CatalogServiceImpl(RemoteMessageDispatcherRegistry dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public CompletableFuture<StatusResult<byte[]>> requestCatalog(String counterPartyId, String counterPartyAddress, String protocol, QuerySpec querySpec) {
        var request = CatalogRequestMessage.Builder.newInstance()
                .protocol(protocol)
                .counterPartyId(counterPartyId)
                .counterPartyAddress(counterPartyAddress)
                .querySpec(querySpec)
                .build();

        return dispatcher.dispatch(byte[].class, request);
    }

    @Override
    public CompletableFuture<StatusResult<byte[]>> requestDataset(String id, String counterPartyId, String counterPartyAddress, String protocol) {
        var request = DatasetRequestMessage.Builder.newInstance()
                .datasetId(id)
                .protocol(protocol)
                .counterPartyId(counterPartyId)
                .counterPartyAddress(counterPartyAddress)
                .build();

        return dispatcher.dispatch(byte[].class, request);
    }
}
