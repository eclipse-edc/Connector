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
        var builder = jsonFactory.createObjectBuilder()
                .add(ID, asset.getId())
                .add(TYPE, Asset.EDC_ASSET_TYPE);

        var propBuilder = jsonFactory.createObjectBuilder();
        transformProperties(asset.getProperties(), propBuilder, mapper, context);
        builder.add(Asset.EDC_ASSET_PROPERTIES, propBuilder);

        if (asset.getPrivateProperties() != null && !asset.getPrivateProperties().isEmpty()) {
            var privatePropBuilder = jsonFactory.createObjectBuilder();
            transformProperties(asset.getPrivateProperties(), privatePropBuilder, mapper, context);
            builder.add(Asset.EDC_ASSET_PRIVATE_PROPERTIES, privatePropBuilder);
        }

        if (asset.getDataAddress() != null) {
            builder.add(Asset.EDC_ASSET_DATA_ADDRESS, context.transform(asset.getDataAddress(), JsonObject.class));
        }

        return builder.build();
    }
}
