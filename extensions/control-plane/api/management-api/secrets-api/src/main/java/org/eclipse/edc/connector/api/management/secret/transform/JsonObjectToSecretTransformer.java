/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.api.management.secret.transform;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.secret.Secret;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_VALUE;


public class JsonObjectToSecretTransformer extends AbstractJsonLdTransformer<JsonObject, Secret> {
    public JsonObjectToSecretTransformer() {
        super(JsonObject.class, Secret.class);
    }

    @Override
    public @Nullable Secret transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = Secret.Builder.newInstance();
        builder.id(nodeId(jsonObject));
        visitProperties(jsonObject, (key, value) -> transformProperties(key, value, builder, context));
        return builder.build();
    }

    private void transformProperties(String key, JsonValue value, Secret.Builder builder, @NotNull TransformerContext context) {
        if (ID.equals(key)) {
            builder.id(transformString(value, context));
        } else if (EDC_SECRET_VALUE.equals(key)) {
            builder.value(transformString(value, context));
        }
    }
}
