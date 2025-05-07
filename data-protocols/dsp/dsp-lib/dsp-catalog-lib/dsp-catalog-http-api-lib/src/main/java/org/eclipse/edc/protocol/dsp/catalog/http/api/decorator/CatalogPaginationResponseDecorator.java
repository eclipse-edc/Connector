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

import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.protocol.dsp.http.spi.message.ContinuationTokenSerDes;
import org.eclipse.edc.protocol.dsp.http.spi.message.ResponseDecorator;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;

import java.net.URI;
import java.util.function.IntBinaryOperator;

public class CatalogPaginationResponseDecorator implements ResponseDecorator<CatalogRequestMessage, Catalog> {

    private static final String NEXT = "next";
    private static final String PREV = "prev";

    private final String requestUrl;
    private final ContinuationTokenSerDes continuationTokenSerDes;
    private final Monitor monitor;

    public CatalogPaginationResponseDecorator(String requestUrl, ContinuationTokenSerDes continuationTokenSerDes, Monitor monitor) {
        this.requestUrl = requestUrl;
        this.continuationTokenSerDes = continuationTokenSerDes;
        this.monitor = monitor;
    }

    @Override
    public Response.ResponseBuilder decorate(Response.ResponseBuilder responseBuilder, CatalogRequestMessage requestBody, Catalog responseBody) {
        var currentQuerySpec = requestBody.getQuerySpec();
        if (responseBody.getDatasets().size() == currentQuerySpec.getLimit()) {
            addLink(NEXT, responseBuilder, currentQuerySpec, (offset, limit) -> offset + limit);
        }

        if (currentQuerySpec.getOffset() >= currentQuerySpec.getLimit()) {
            addLink(PREV, responseBuilder, currentQuerySpec, (offset, limit) -> offset - limit);
        }

        return responseBuilder;
    }

    private void addLink(String rel, Response.ResponseBuilder responseBuilder, QuerySpec currentQuerySpec, IntBinaryOperator newOffsetOperator) {
        var newOffset = newOffsetOperator.applyAsInt(currentQuerySpec.getOffset(), currentQuerySpec.getLimit());
        continuationTokenSerDes.serialize(currentQuerySpec.toBuilder().offset(newOffset).build())
                .onSuccess(token -> responseBuilder.link(URI.create(requestUrl + "?continuationToken=" + token), rel))
                .onFailure(failure -> monitor.warning("Cannot serialize continuationToken for catalog pagination: " + failure.getFailureDetail()));
    }
}
