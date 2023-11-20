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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import jakarta.json.JsonObject;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.identitytrust.model.VerifiablePresentation;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unchecked")
public class JwtToVerifiablePresentationTransformer implements TypeTransformer<String, VerifiablePresentation> {
    private static final String VP_CLAIM = "vp";
    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final JsonLd jsonLd;

    public JwtToVerifiablePresentationTransformer(Monitor monitor, ObjectMapper objectMapper, JsonLd jsonLd) {
        this.monitor = monitor;
        this.objectMapper = objectMapper;
        this.jsonLd = jsonLd;
    }

    @Override
    public Class<String> getInputType() {
        return String.class;
    }

    @Override
    public Class<VerifiablePresentation> getOutputType() {
        return VerifiablePresentation.class;
    }

    @Override
    public @Nullable VerifiablePresentation transform(@NotNull String jsonWebToken, @NotNull TransformerContext context) {

        var builder = VerifiablePresentation.Builder.newInstance();
        try {
            var signedJwt = SignedJWT.parse(jsonWebToken);
            var claimsSet = signedJwt.getJWTClaimsSet();

            var vpObject = claimsSet.getClaim(VP_CLAIM);
            builder.holder(claimsSet.getIssuer());
            builder.id(claimsSet.getJWTID());

            if (vpObject instanceof Map map) {
                builder.types((List<String>) map.get("type"));

                var credentialObject = map.get("verifiableCredential");
                builder.credentials(extractCredentials(credentialObject, context));
                return builder.build();
            }

        } catch (ParseException e) {
            monitor.warning("Error parsing JWT", e);
            context.reportProblem("Error parsing JWT: %s".formatted(e.getMessage()));
        }
        context.reportProblem("Could not parse VerifiablePresentation from JWT.");
        return null;
    }

    private List<VerifiableCredential> extractCredentials(Object credentialsObject, TransformerContext context) {
        Collection<?> list;
        if (credentialsObject instanceof Collection<?>) {
            list = (Collection<?>) credentialsObject;
        } else {
            list = List.of(credentialsObject);
        }

        return list.stream().map(obj -> {
            if (obj instanceof String) { // VC is JWT
                return context.transform(obj.toString(), VerifiableCredential.class);
            } else { // VC is LDP
                var input = objectMapper.convertValue(obj, JsonObject.class);
                var expansion = jsonLd.expand(input);
                if (expansion.succeeded()) {
                    return context.transform(expansion.getContent(), VerifiableCredential.class);
                }
                context.reportProblem("Error expanding embedded VC: %s".formatted(expansion.getFailureDetail()));
                return null;
            }

        }).filter(Objects::nonNull).toList();
    }
}
