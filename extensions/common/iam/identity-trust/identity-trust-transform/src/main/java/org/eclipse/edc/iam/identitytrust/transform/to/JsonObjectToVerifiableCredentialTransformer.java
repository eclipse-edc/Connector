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
import jakarta.json.JsonValue;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.CredentialStatus;
import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

import static org.eclipse.edc.identitytrust.model.VerifiableCredential.Builder;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_DESCRIPTION_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_EXPIRATIONDATE_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_ISSUANCEDATE_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_ISSUER_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_NAME_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_STATUS_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_SUBJECT_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_VALIDFROM_PROPERTY;
import static org.eclipse.edc.identitytrust.model.VerifiableCredential.VERIFIABLE_CREDENTIAL_VALIDUNTIL_PROPERTY;

public class JsonObjectToVerifiableCredentialTransformer extends AbstractJsonLdTransformer<JsonObject, VerifiableCredential> {
    private final ObjectMapper mapper;
    private final JsonLd jsonLd;

    public JsonObjectToVerifiableCredentialTransformer(ObjectMapper jsonLdMapper, JsonLd jsonLd) {
        super(JsonObject.class, VerifiableCredential.class);
        this.mapper = jsonLdMapper;
        this.jsonLd = jsonLd;
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
        var expansionResult = jsonLd.expand(jsonObject);
        if (expansionResult.failed()) {
            context.reportProblem("Could not expand JSON-LD of the VerifiableCredential: %s".formatted(expansionResult.getFailureDetail()));
            return null;
        }
        jsonObject = expansionResult.getContent();


        var vcBuilder = Builder.newInstance(rawVc, CredentialFormat.JSON_LD);
        vcBuilder.id(nodeId(jsonObject));
        transformArrayOrObject(jsonObject.get(JsonLdKeywords.TYPE), Object.class, o -> vcBuilder.type(o.toString()), context);

        visitProperties(jsonObject, (s, jsonValue) -> transformProperties(s, jsonValue, vcBuilder, context));
        return vcBuilder.build();
    }

    private void transformProperties(String key, JsonValue jsonValue, Builder vcBuilder, TransformerContext context) {
        switch (key) {
            case VERIFIABLE_CREDENTIAL_DESCRIPTION_PROPERTY ->
                    vcBuilder.description(transformString(jsonValue, context));
            case VERIFIABLE_CREDENTIAL_ISSUER_PROPERTY -> vcBuilder.issuer(parseIssuer(jsonValue, context));
            case VERIFIABLE_CREDENTIAL_VALIDFROM_PROPERTY, VERIFIABLE_CREDENTIAL_ISSUANCEDATE_PROPERTY ->
                    vcBuilder.issuanceDate(parseDate(jsonValue, context));
            case VERIFIABLE_CREDENTIAL_VALIDUNTIL_PROPERTY, VERIFIABLE_CREDENTIAL_EXPIRATIONDATE_PROPERTY ->
                    vcBuilder.expirationDate(parseDate(jsonValue, context));
            case VERIFIABLE_CREDENTIAL_STATUS_PROPERTY ->
                    vcBuilder.credentialStatus(context.transform(jsonValue.asJsonObject(), CredentialStatus.class));
            case VERIFIABLE_CREDENTIAL_SUBJECT_PROPERTY ->
                    vcBuilder.credentialSubject(transformArray(jsonValue, CredentialSubject.class, context));
            case VERIFIABLE_CREDENTIAL_NAME_PROPERTY -> vcBuilder.name(transformString(jsonValue, context));

            default ->
                    context.reportProblem("Unknown property: %s type: %s".formatted(key, jsonValue.getValueType().name()));
        }
    }

    private Instant parseDate(JsonValue jsonValue, TransformerContext context) {
        var str = transformString(jsonValue, context);
        return Instant.parse(str);
    }

    private Object parseIssuer(JsonValue jsonValue, TransformerContext context) {
        return transformString(jsonValue, context); //todo: handle the case where the issuer is an object
    }
}
