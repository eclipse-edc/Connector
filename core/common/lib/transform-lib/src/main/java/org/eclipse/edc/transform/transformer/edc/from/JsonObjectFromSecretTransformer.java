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

package org.eclipse.edc.transform.transformer.edc.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.secret.Secret;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_VALUE;
import static org.eclipse.edc.spi.types.domain.secret.Secret.PROPERTY_ID;

public class JsonObjectFromSecretTransformer extends AbstractJsonLdTransformer<Secret, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromSecretTransformer(JsonBuilderFactory jsonFactory) {
        super(Secret.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull Secret secret, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(PROPERTY_ID, secret.getId())
                .add(TYPE, Secret.EDC_SECRET_TYPE)
                .add(EDC_SECRET_VALUE, secret.getValue());

        return builder.build();
    }
}
