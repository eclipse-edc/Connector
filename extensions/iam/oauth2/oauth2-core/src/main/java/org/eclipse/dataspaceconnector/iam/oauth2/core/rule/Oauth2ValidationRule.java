/*
 *  Copyright (c) 2020 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core.rule;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.common.token.TokenValidationRule;
import org.eclipse.dataspaceconnector.iam.oauth2.core.Oauth2Configuration;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.time.ZoneOffset.UTC;

public class Oauth2ValidationRule implements TokenValidationRule {
    private final Oauth2Configuration configuration;

    public Oauth2ValidationRule(Oauth2Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Validates the JWT by checking the audience, nbf, and expiration. Accessible for testing.
     *
     * @param toVerify   The jwt including the claims.
     * @param additional No more additional information needed for this validation, can be null.
     */
    @Override
    public Result<SignedJWT> checkRule(SignedJWT toVerify, @Nullable Map<String, Object> additional) {
        try {
            JWTClaimsSet claimsSet = toVerify.getJWTClaimsSet();
            List<String> errors = new ArrayList<>();
            String audience = configuration.getProviderAudience();
            List<String> audiences = claimsSet.getAudience();
            if (audiences.isEmpty()) {
                errors.add("Required audience (aud) claim is missing in token");
            } else if (!audiences.contains(audience)) {
                errors.add("Token audience (aud) claim did not contain connector audience: " + audience);
            }

            Instant nowUtc = Instant.now();
            var leewayNow = nowUtc.plusSeconds(configuration.getNotBeforeValidationLeeway());
            var notBefore = claimsSet.getNotBeforeTime();
            if (notBefore == null) {
                errors.add("Required not before (nbf) claim is missing in token");
            } else if (leewayNow.isBefore(convertToUtcTime(notBefore))) {
                errors.add("Current date/time with leeway before the not before (nbf) claim in token");
            }

            Date expires = claimsSet.getExpirationTime();
            var expiresSet = expires != null;
            if (!expiresSet) {
                errors.add("Required expiration time (exp) claim is missing in token");
            } else if (nowUtc.isAfter(convertToUtcTime(expires))) {
                errors.add("Token has expired (exp)");
            }

            Date issuedAt = claimsSet.getIssueTime();
            if (issuedAt != null) {
                if (expiresSet && issuedAt.toInstant().isAfter(expires.toInstant())) {
                    errors.add("Issued at (iat) claim is after expiration time (exp) claim in token");
                } else if (nowUtc.isBefore(convertToUtcTime(issuedAt))) {
                    errors.add("Current date/time before issued at (iat) claim in token");
                }
            }

            if (errors.isEmpty()) {
                return Result.success(toVerify);
            } else {
                return Result.failure(errors);
            }
        } catch (ParseException e) {
            throw new EdcException("Oauth2ValidationRule: unable to parse SignedJWT (" + e.getMessage() + ")");
        }
    }

    private static Instant convertToUtcTime(Date date) {
        return ZonedDateTime.ofInstant(date.toInstant(), UTC).toInstant();
    }
}
