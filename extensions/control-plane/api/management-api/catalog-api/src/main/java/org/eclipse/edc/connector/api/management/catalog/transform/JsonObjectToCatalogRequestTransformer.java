/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.catalog.transform;

import jakarta.json.JsonObject;
import org.eclipse.edc.catalog.spi.CatalogRequest;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_PROTOCOL;
import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_PROVIDER_URL;
import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_QUERY_SPEC;
import static org.eclipse.edc.catalog.spi.CatalogRequest.CATALOG_REQUEST_TYPE;

public class JsonObjectToCatalogRequestTransformer extends AbstractJsonLdTransformer<JsonObject, CatalogRequest> {

    private final Monitor monitor;

    public JsonObjectToCatalogRequestTransformer(Monitor monitor) {
        super(JsonObject.class, CatalogRequest.class);
        this.monitor = monitor;
    }

    @Override
    public @Nullable CatalogRequest transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var counterPartyAddress = Optional.of(object)
                .map(it -> it.get(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS))
                .orElseGet(() -> {
                    monitor.warning(format("The attribute %s has been deprecated in type %s, please use %s",
                            CATALOG_REQUEST_PROVIDER_URL, CATALOG_REQUEST_TYPE, CATALOG_REQUEST_COUNTER_PARTY_ADDRESS));
                    return object.get(CATALOG_REQUEST_PROVIDER_URL);
                });

        return CatalogRequest.Builder.newInstance()
                .protocol(transformString(object.get(CATALOG_REQUEST_PROTOCOL), context))
                .querySpec(transformObject(object.get(CATALOG_REQUEST_QUERY_SPEC), QuerySpec.class, context))
                .counterPartyAddress(transformString(counterPartyAddress, context))
                .build();
    }

}
