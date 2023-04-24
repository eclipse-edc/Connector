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
import org.eclipse.edc.jsonld.spi.PropertyAndTypeNames;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Converts from an {@link Asset} as a {@link JsonObject} in JSON-LD expanded form to an {@link Asset}.
 */
public class JsonObjectToAssetTransformer extends AbstractJsonLdTransformer<JsonObject, Asset> {
    protected JsonObjectToAssetTransformer() {
        super(JsonObject.class, Asset.class);
    }

    @Override
    public @Nullable Asset transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = Asset.Builder.newInstance();
        builder.id(nodeId(jsonObject));
        visitProperties(jsonObject, (s, jsonValue) -> transformProperties(s, jsonValue, builder, context));
        return builderResult(builder::build, context);
    }

    private void transformProperties(String key, JsonValue jsonValue, Asset.Builder builder, TransformerContext context) {
        if (PropertyAndTypeNames.EDC_ASSET_PROPERTIES.equals(key) && jsonValue instanceof JsonArray) {
            var props = jsonValue.asJsonArray().getJsonObject(0);
            // indirect recursion - parse the properties map and re-engage the visitor
            visitProperties(props, (k, val) -> transformProperties(k, val, builder, context));

            //parse known properties:
        } else if (PropertyAndTypeNames.EDC_ASSET_NAME.equals(key)) {
            transformString(jsonValue, builder::name, context);
        } else if (PropertyAndTypeNames.EDC_ASSET_DESCRIPTION.equals(key)) {
            transformString(jsonValue, builder::description, context);
        } else if (PropertyAndTypeNames.EDC_ASSET_VERSION.equals(key)) {
            transformString(jsonValue, builder::version, context);
        } else if (PropertyAndTypeNames.EDC_ASSET_CONTENTTYPE.equals(key)) {
            transformString(jsonValue, builder::contentType, context);
        } else {
            builder.property(key, transformGenericProperty(jsonValue, context));
        }

    }
}
