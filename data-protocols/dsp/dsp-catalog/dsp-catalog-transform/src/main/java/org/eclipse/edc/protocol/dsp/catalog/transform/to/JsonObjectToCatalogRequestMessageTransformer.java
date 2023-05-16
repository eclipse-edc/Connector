/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.transform.to;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_FILTER_PROPERTY;

/**
 * Transforms a {@link JsonObject} in JSON-LD expanded form to a {@link CatalogRequestMessage}.
 */
public class JsonObjectToCatalogRequestMessageTransformer extends AbstractJsonLdTransformer<JsonObject, CatalogRequestMessage> {

    private final ObjectMapper mapper;

    public JsonObjectToCatalogRequestMessageTransformer(ObjectMapper mapper) {
        super(JsonObject.class, CatalogRequestMessage.class);
        this.mapper = mapper;
    }

    @Override
    public @Nullable CatalogRequestMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = CatalogRequestMessage.Builder.newInstance();

        var querySpec = transformQuerySpec(object, context);
        if (querySpec != null) {
            builder.querySpec(querySpec);
        }

        return builder.build();
    }

    @Nullable
    private QuerySpec transformQuerySpec(JsonObject object, TransformerContext context) {
        var value = object.get(DSPACE_FILTER_PROPERTY);
        if (value == null) {
            return null;
        }
        return transformObject(value, QuerySpec.class, context);
    }
}
