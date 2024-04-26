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

package org.eclipse.edc.connector.controlplane.api.management.catalog.transform;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ID;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_PROTOCOL;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_QUERY_SPEC;

public class JsonObjectToCatalogRequestTransformer extends AbstractJsonLdTransformer<JsonObject, CatalogRequest> {

    public JsonObjectToCatalogRequestTransformer() {
        super(JsonObject.class, CatalogRequest.class);
    }

    @Override
    public @Nullable CatalogRequest transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var counterPartyAddress = transformString(object.get(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS), context);

        // For backward compatibility if the ID is not sent, fallback to the counterPartyAddress
        var counterPartyId = Optional.ofNullable(object.get(CATALOG_REQUEST_COUNTER_PARTY_ID))
                .map(it -> transformString(it, context))
                .orElse(counterPartyAddress);

        var querySpec = Optional.of(object)
                .map(it -> it.get(CATALOG_REQUEST_QUERY_SPEC))
                .map(it -> transformObject(it, QuerySpec.class, context))
                .orElse(null);

        return CatalogRequest.Builder.newInstance()
                .protocol(transformString(object.get(CATALOG_REQUEST_PROTOCOL), context))
                .counterPartyAddress(counterPartyAddress)
                .counterPartyId(counterPartyId)
                .querySpec(querySpec)
                .build();
    }

}
