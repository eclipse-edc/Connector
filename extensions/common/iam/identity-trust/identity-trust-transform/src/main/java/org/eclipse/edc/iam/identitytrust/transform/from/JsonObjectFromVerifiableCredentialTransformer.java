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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObjectFromVerifiableCredentialTransformer extends AbstractJsonLdTransformer<VerifiableCredential, JsonObject> {
    private final ObjectMapper mapper;

    public JsonObjectFromVerifiableCredentialTransformer(ObjectMapper jsonLdMapper) {
        super(VerifiableCredential.class, JsonObject.class);
        this.mapper = jsonLdMapper;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull VerifiableCredential credential, @NotNull TransformerContext context) {
        // we can't actually transform anything, instead we MUST use the raw VC value, if the format is JSON_LD. Otherwise,
        // the proof is likely not valid anymore
        if (credential.getFormat() == CredentialFormat.JSON_LD) {
            try {
                return mapper.readValue(credential.getRawVc(), JsonObject.class);
            } catch (JsonProcessingException e) {
                context.reportProblem("Cannot convert raw VC to JSON-LD: %s".formatted(e.getMessage()));
            }
        }

        // todo: should this throw an exception instead?
        context.reportProblem("This VerifiableCredential is expected to be in JSON_LD format but was %s".formatted(credential.getFormat().toString()));
        return null;
    }
}
