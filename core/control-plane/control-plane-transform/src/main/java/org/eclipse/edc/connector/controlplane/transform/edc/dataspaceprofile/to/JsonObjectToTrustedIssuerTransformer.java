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

package org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.spi.TrustedIssuer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class JsonObjectToTrustedIssuerTransformer extends AbstractJsonLdTransformer<JsonObject, TrustedIssuer> {

    public JsonObjectToTrustedIssuerTransformer() {
        super(JsonObject.class, TrustedIssuer.class);
    }

    @Override
    public @Nullable TrustedIssuer transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = TrustedIssuer.Builder.newInstance()
                .id(nodeId(jsonObject));

        Optional.ofNullable(jsonObject.getJsonArray(TrustedIssuer.TRUSTED_ISSUER_SUPPORTED_TYPES_IRI))
                .ifPresent(types -> builder.supportedTypes(types.stream().map(v -> transformString(v, context)).toList()));

        return builder.build();
    }
}
