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

import jakarta.json.JsonObject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSchema;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;

import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSchema.CREDENTIAL_SCHEMA_ID_PROPERTY;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSchema.CREDENTIAL_SCHEMA_TYPE_PROPERTY;


public class JsonObjectToCredentialSchemaTransformer extends AbstractJsonLdTransformer<JsonObject, CredentialSchema> {
    public JsonObjectToCredentialSchemaTransformer() {
        super(JsonObject.class, CredentialSchema.class);
    }

    @Override
    public @Nullable CredentialSchema transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {

        var id = nodeId(jsonObject);

        try {
            new URI(id);
        } catch (URISyntaxException ignored) {
            context.reportProblem("The '%s' property must be in URI format but was not: '%s'".formatted(CREDENTIAL_SCHEMA_ID_PROPERTY, id));
        }

        var type = transformString(jsonObject.get(CREDENTIAL_SCHEMA_TYPE_PROPERTY), context);
        if (type == null) {
            context.reportProblem("The '%s' property is mandatory on credentialSchema objects".formatted(CREDENTIAL_SCHEMA_TYPE_PROPERTY));
        }
        return new CredentialSchema(id, type);
    }
}
