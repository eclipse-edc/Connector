/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.spi.TrustedIssuer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.spi.TrustedIssuer.TRUSTED_ISSUER_SUPPORTED_TYPES_IRI;
import static org.eclipse.edc.protocol.spi.TrustedIssuer.TRUSTED_ISSUER_TYPE_IRI;

public class JsonObjectFromTrustedIssuerTransformer extends AbstractJsonLdTransformer<TrustedIssuer, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromTrustedIssuerTransformer(JsonBuilderFactory jsonFactory) {
        super(TrustedIssuer.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull TrustedIssuer trustedIssuer, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(ID, trustedIssuer.getId())
                .add(TYPE, TRUSTED_ISSUER_TYPE_IRI)
                .add(TRUSTED_ISSUER_SUPPORTED_TYPES_IRI, trustedIssuer.getSupportedTypes().stream()
                        .map(Json::createValue)
                        .collect(toJsonArray()))
                .build();
    }
}
