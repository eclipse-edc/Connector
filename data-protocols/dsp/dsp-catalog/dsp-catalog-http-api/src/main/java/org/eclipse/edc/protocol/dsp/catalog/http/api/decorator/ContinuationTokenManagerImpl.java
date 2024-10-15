/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.http.api.decorator;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenManager;
import org.eclipse.edc.protocol.dsp.http.spi.message.ResponseDecorator;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;

import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_PROPERTY_FILTER_IRI;

public class ContinuationTokenManagerImpl implements ContinuationTokenManager {

    private final Base64continuationTokenSerDes continuationTokenSerDes;
    private final Monitor monitor;

    public ContinuationTokenManagerImpl(Base64continuationTokenSerDes continuationTokenSerDes, Monitor monitor) {
        this.continuationTokenSerDes = continuationTokenSerDes;
        this.monitor = monitor;
    }

    @Override
    public Result<JsonObject> applyQueryFromToken(JsonObject requestMessage, String continuationToken) {
        return continuationTokenSerDes.deserialize(continuationToken)
                .map(query -> Json.createArrayBuilder().add(query))
                .map(filter -> Json.createObjectBuilder(requestMessage).add(DSPACE_PROPERTY_FILTER_IRI, filter))
                .map(JsonObjectBuilder::build);
    }

    @Override
    public ResponseDecorator<CatalogRequestMessage, Catalog> createResponseDecorator(String requestUrl) {
        return new CatalogPaginationResponseDecorator(requestUrl, continuationTokenSerDes, monitor);
    }
}
