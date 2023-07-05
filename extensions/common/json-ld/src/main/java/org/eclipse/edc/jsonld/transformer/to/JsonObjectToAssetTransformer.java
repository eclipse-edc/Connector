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

package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.spi.types.domain.asset.Asset.EDC_ASSET_DATA_ADDRESS;
import static org.eclipse.edc.spi.types.domain.asset.Asset.EDC_ASSET_PRIVATE_PROPERTIES;

/**
 * Converts from an {@link Asset} as a {@link JsonObject} in JSON-LD expanded form to an {@link Asset}.
 */
public class JsonObjectToAssetTransformer extends AbstractJsonLdTransformer<JsonObject, Asset> {
    public JsonObjectToAssetTransformer() {
        super(JsonObject.class, Asset.class);
    }

    @Override
    public @Nullable Asset transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = Asset.Builder.newInstance();
        builder.id(nodeId(jsonObject));
        visitProperties(jsonObject, (s, jsonValue) -> transformProperties(s, jsonValue, builder, context, false));
        return builderResult(builder::build, context);
    }

    private void transformProperties(String key, JsonValue jsonValue, Asset.Builder builder, TransformerContext context, boolean isPrivate) {
        if (Asset.EDC_ASSET_PROPERTIES.equals(key) && jsonValue instanceof JsonArray) {
            var props = jsonValue.asJsonArray().getJsonObject(0);
            // indirect recursion - parse the properties map and re-engage the visitor
            visitProperties(props, (k, val) -> transformProperties(k, val, builder, context, false));
        } else if (EDC_ASSET_PRIVATE_PROPERTIES.equals(key) && jsonValue instanceof JsonArray) {
            var props = jsonValue.asJsonArray().getJsonObject(0);
            visitProperties(props, (k, val) -> transformProperties(k, val, builder, context, true));
        } else if (EDC_ASSET_DATA_ADDRESS.equals(key) && jsonValue instanceof JsonArray) {
            builder.dataAddress(transformObject(jsonValue, DataAddress.class, context));
        } else {
            if (isPrivate) {
                builder.privateProperty(key, transformGenericProperty(jsonValue, context));
            } else {
                builder.property(key, transformGenericProperty(jsonValue, context));
            }
        }

    }
}
