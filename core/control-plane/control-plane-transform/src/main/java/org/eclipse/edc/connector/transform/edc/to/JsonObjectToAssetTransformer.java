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

package org.eclipse.edc.connector.transform.edc.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.asset.spi.domain.Asset;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

import static org.eclipse.edc.connector.asset.spi.domain.Asset.EDC_ASSET_DATA_ADDRESS;
import static org.eclipse.edc.connector.asset.spi.domain.Asset.EDC_ASSET_PRIVATE_PROPERTIES;
import static org.eclipse.edc.connector.asset.spi.domain.Asset.EDC_ASSET_PROPERTIES;

/**
 * Converts from an {@link Asset} as a {@link JsonObject} in JSON-LD expanded form to an {@link Asset}.
 */
public class JsonObjectToAssetTransformer extends AbstractJsonLdTransformer<JsonObject, Asset> {
    public JsonObjectToAssetTransformer() {
        super(JsonObject.class, Asset.class);
    }

    @Override
    public @Nullable Asset transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = Asset.Builder.newInstance()
                .id(nodeId(jsonObject));

        visitProperties(jsonObject, key -> switch (key) {
            case EDC_ASSET_PROPERTIES -> value -> visitProperties(value.asJsonArray().getJsonObject(0), property(context, builder));
            case EDC_ASSET_PRIVATE_PROPERTIES -> value -> visitProperties(value.asJsonArray().getJsonObject(0), privateProperty(context, builder));
            case EDC_ASSET_DATA_ADDRESS -> value -> builder.dataAddress(transformObject(value, DataAddress.class, context));
            default -> doNothing();
        });

        return builderResult(builder::build, context);
    }

    @NotNull
    private BiConsumer<String, JsonValue> privateProperty(@NotNull TransformerContext context, Asset.Builder builder) {
        return (k, val) -> builder.privateProperty(k, transformGenericProperty(val, context));
    }

    @NotNull
    private BiConsumer<String, JsonValue> property(@NotNull TransformerContext context, Asset.Builder builder) {
        return (k, val) -> builder.property(k, transformGenericProperty(val, context));
    }

}
