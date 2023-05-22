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
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.connector.api.management.catalog.model.CatalogRequestDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_PROTOCOL;
import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_PROVIDER_URL;
import static org.eclipse.edc.catalog.spi.CatalogRequest.EDC_CATALOG_REQUEST_QUERY_SPEC;

public class JsonObjectToCatalogRequestDtoTransformer extends AbstractJsonLdTransformer<JsonObject, CatalogRequestDto> {

    public JsonObjectToCatalogRequestDtoTransformer() {
        super(JsonObject.class, CatalogRequestDto.class);
    }

    @Override
    public @Nullable CatalogRequestDto transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = CatalogRequestDto.Builder.newInstance();

        visitProperties(object, key -> {
            switch (key) {
                case EDC_CATALOG_REQUEST_PROTOCOL:
                    return v -> builder.protocol(transformString(v, context));
                case EDC_CATALOG_REQUEST_PROVIDER_URL:
                    return v -> builder.providerUrl(transformString(v, context));
                case EDC_CATALOG_REQUEST_QUERY_SPEC:
                    return v -> builder.querySpec(transformObject(v, QuerySpecDto.class, context));
                default:
                    return doNothing();
            }
        });

        return builder.build();
    }

}
