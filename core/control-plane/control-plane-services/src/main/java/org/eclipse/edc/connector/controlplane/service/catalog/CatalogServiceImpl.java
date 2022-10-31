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

package org.eclipse.edc.connector.controlplane.service.catalog;

import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.CatalogRequest;
import org.eclipse.edc.connector.controlplane.spi.catalog.CatalogService;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.concurrent.CompletableFuture;

public class CatalogServiceImpl implements CatalogService {

    private final RemoteMessageDispatcherRegistry dispatcher;

    public CatalogServiceImpl(RemoteMessageDispatcherRegistry dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public CompletableFuture<Catalog> getByProviderUrl(String providerUrl, QuerySpec spec) {
        var request = CatalogRequest.Builder.newInstance()
                .protocol("ids-multipart")
                .connectorId(providerUrl)
                .connectorAddress(providerUrl)
                .querySpec(spec)
                .build();

        return dispatcher.send(Catalog.class, request, () -> null);
    }
}
