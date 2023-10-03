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

package org.eclipse.edc.iam.identitytrust.transform.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.identitytrust.model.CredentialStatus;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObjectFromCredentialStatusTransformer extends AbstractJsonLdTransformer<CredentialStatus, JsonObject> {
    private final JsonBuilderFactory factory;
    private final ObjectMapper mapper;

    public JsonObjectFromCredentialStatusTransformer(JsonBuilderFactory factory, ObjectMapper jsonLdMapper) {
        super(CredentialStatus.class, JsonObject.class);
        this.factory = factory;
        this.mapper = jsonLdMapper;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull CredentialStatus credentialStatus, @NotNull TransformerContext context) {
        var jsonBuilder = factory.createObjectBuilder()
                .add(CredentialStatus.CREDENTIAL_STATUS_ID_PROPERTY, credentialStatus.id())
                .add(CredentialStatus.CREDENTIAL_STATUS_TYPE_PROPERTY, credentialStatus.type());

        if (credentialStatus.additionalProperties() != null) {
            transformProperties(credentialStatus.additionalProperties(), jsonBuilder, mapper, context);
        }

        return jsonBuilder.build();
    }
}
