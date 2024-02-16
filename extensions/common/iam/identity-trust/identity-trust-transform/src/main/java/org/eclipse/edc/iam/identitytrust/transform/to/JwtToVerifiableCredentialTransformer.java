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

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.identitytrust.model.CredentialStatus;
import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.Issuer;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class JwtToVerifiableCredentialTransformer implements TypeTransformer<String, VerifiableCredential> {
    public static final String EXPIRATION_DATE_PROPERTY = "expirationDate";
    public static final String ISSUANCE_DATE_PROPERTY = "issuanceDate";
    public static final String ID_PROPERTY = "id";
    public static final String TYPE_PROPERTY = "type";
    public static final String TYPE_CLAIM = TYPE_PROPERTY;
    private static final String VC_CLAIM = "vc";
    private static final String SUBJECT_CLAIM = "credentialSubject";
    private static final String CREDENTIAL_STATUS_PROPERTY = "credentialStatus";
    private final Monitor monitor;

    public JwtToVerifiableCredentialTransformer(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public Class<String> getInputType() {
        return String.class;
    }

    @Override
    public Class<VerifiableCredential> getOutputType() {
        return VerifiableCredential.class;
    }

    @Override
    public @Nullable VerifiableCredential transform(@NotNull String serializedJwt, @NotNull TransformerContext context) {
        try {
            var jwt = SignedJWT.parse(serializedJwt);
            var claims = jwt.getJWTClaimsSet();
            var builder = VerifiableCredential.Builder.newInstance();

            var vcObject = claims.getClaim(VC_CLAIM);
            if (vcObject instanceof Map vc) {
                builder.types((List<String>) vc.get(TYPE_CLAIM));
                builder.credentialSubject(extractSubject((Map<String, ?>) vc.get(SUBJECT_CLAIM)));

                getExpirationDate(vc, builder, claims);
                getIssuanceDate(vc, builder, claims);

                builder.issuer(new Issuer(claims.getIssuer(), Map.of()));
                builder.credentialStatus(extractStatus((Map<String, Object>) vc.get(CREDENTIAL_STATUS_PROPERTY)));
                builder.name(claims.getSubject()); // todo: is this correct?
                return builder.build();
            }
        } catch (ParseException e) {
            monitor.warning("Error parsing JWT", e);
            context.reportProblem("Error parsing JWT: %s".formatted(e.getMessage()));
        }
        return null;
    }

    private void getIssuanceDate(Map vcObject, VerifiableCredential.Builder builder, JWTClaimsSet fallback) {
        builder.issuanceDate(ofNullable(vcObject.get(ISSUANCE_DATE_PROPERTY))
                .map(this::toInstant)
                .orElseGet(() -> fallback.getIssueTime().toInstant()));
    }

    /**
     * Expiration date is entirely optional, take it either from the VC or from the JWT
     */
    private void getExpirationDate(Map vcObject, VerifiableCredential.Builder builder, JWTClaimsSet fallback) {
        builder.expirationDate(ofNullable(vcObject.get(EXPIRATION_DATE_PROPERTY))
                .map(this::toInstant)
                .orElseGet(() -> ofNullable(fallback.getExpirationTime()).map(Date::toInstant).orElse(null)));
    }

    private CredentialStatus extractStatus(Map<String, Object> status) {
        if (status == null || status.isEmpty()) {
            return null;
        }
        var id = status.remove(ID_PROPERTY).toString();
        var type = status.remove(TYPE_PROPERTY).toString();

        return new CredentialStatus(id, type, status);
    }

    private CredentialSubject extractSubject(Map<String, ?> subject) {
        var bldr = CredentialSubject.Builder.newInstance();
        subject.entrySet().forEach(e -> bldr.claim(e.getKey(), e.getValue()));
        return bldr.build();
    }

    private Instant toInstant(Object stringOrMap) {
        var str = stringOrMap.toString();

        if (stringOrMap instanceof Map) {
            str = ((Map) stringOrMap).get("value").toString();
        }
        return Instant.parse(str);
    }
}
