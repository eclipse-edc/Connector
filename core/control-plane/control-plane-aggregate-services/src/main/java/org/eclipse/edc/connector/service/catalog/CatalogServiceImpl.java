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

package org.eclipse.edc.connector.service.catalog;

import org.eclipse.edc.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
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
    public CompletableFuture<StatusResult<byte[]>> request(String providerUrl, String protocol, QuerySpec querySpec) {
        var request = CatalogRequestMessage.Builder.newInstance()
                .protocol(protocol)
                .counterPartyAddress(providerUrl)
                .querySpec(querySpec)
                .build();

        return dispatcher.dispatch(byte[].class, request);
    }
}
