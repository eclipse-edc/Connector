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

package org.eclipse.edc.iam.identitytrust.transform.to;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObjectToVerifiableCredentialTransformer extends AbstractJsonLdTransformer<JsonObject, VerifiableCredential> {
    private final ObjectMapper mapper;

    public JsonObjectToVerifiableCredentialTransformer(ObjectMapper jsonLdMapper) {
        super(JsonObject.class, VerifiableCredential.class);
        this.mapper = jsonLdMapper;
    }

    @Override
    public @Nullable VerifiableCredential transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        String rawVc;
        try {
            rawVc = mapper.writeValueAsString(jsonObject);
        } catch (JsonProcessingException e) {
            context.reportProblem("Cannot convert JsonObject to String: %s".formatted(e.getMessage()));
            return null;
        }
        var vc = VerifiableCredential.Builder.newInstance(rawVc, CredentialFormat.JSON_LD);
        return vc.build();
    }
}
