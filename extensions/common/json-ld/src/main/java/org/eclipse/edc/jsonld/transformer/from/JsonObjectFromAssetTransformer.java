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

package org.eclipse.edc.jsonld.transformer.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromAssetTransformer extends AbstractJsonLdTransformer<Asset, JsonObject> {
    private final ObjectMapper mapper;
    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromAssetTransformer(JsonBuilderFactory jsonFactory, ObjectMapper jsonLdMapper) {
        super(Asset.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.mapper = jsonLdMapper;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull Asset asset, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(ID, asset.getId());
        builder.add(TYPE, Asset.EDC_ASSET_TYPE);
        transformProperties(asset.getProperties(), builder, mapper, context);
        return builder.build();
    }
}
