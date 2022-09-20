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

import org.eclipse.dataspaceconnector.iam.oauth2.core.Oauth2Configuration;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationRule;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.AUDIENCE;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.EXPIRATION_TIME;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.ISSUED_AT;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.NOT_BEFORE;

public class Oauth2ValidationRule implements TokenValidationRule {
    private final Oauth2Configuration configuration;
    private final Clock clock;

    public Oauth2ValidationRule(Oauth2Configuration configuration, Clock clock) {
        this.configuration = configuration;
        this.clock = clock;
    }

    /**
     * Validates the JWT by checking the audience, nbf, and expiration. Accessible for testing.
     *
     * @param toVerify   The jwt including the claims.
     * @param additional No more additional information needed for this validation, can be null.
     */
    @Override
    public Result<Void> checkRule(ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var claimsSet = toVerify.getClaims();
        List<String> errors = new ArrayList<>();
        String audience = configuration.getProviderAudience();
        List<String> audiences = Optional.of(claimsSet).map(it -> it.get(AUDIENCE)).map(List.class::cast).orElse(Collections.emptyList());
        if (audiences.isEmpty()) {
            errors.add("Required audience (aud) claim is missing in token");
        } else if (!audiences.contains(audience)) {
            errors.add("Token audience (aud) claim did not contain connector audience: " + audience);
        }

        Instant now = clock.instant();
        var leewayNow = now.plusSeconds(configuration.getNotBeforeValidationLeeway());
        var notBefore = (Date) claimsSet.get(NOT_BEFORE);
        if (notBefore == null) {
            errors.add("Required not before (nbf) claim is missing in token");
        } else if (leewayNow.isBefore(convertToUtcTime(notBefore))) {
            errors.add("Current date/time with leeway before the not before (nbf) claim in token");
        }

        var expires = (Date) claimsSet.get(EXPIRATION_TIME);
        var expiresSet = expires != null;
        if (!expiresSet) {
            errors.add("Required expiration time (exp) claim is missing in token");
        } else if (now.isAfter(convertToUtcTime(expires))) {
            errors.add("Token has expired (exp)");
        }

        var issuedAt = (Date) claimsSet.get(ISSUED_AT);
        if (issuedAt != null) {
            if (expiresSet && issuedAt.toInstant().isAfter(expires.toInstant())) {
                errors.add("Issued at (iat) claim is after expiration time (exp) claim in token");
            } else if (now.isBefore(convertToUtcTime(issuedAt))) {
                errors.add("Current date/time before issued at (iat) claim in token");
            }
        }

        if (errors.isEmpty()) {
            return Result.success();
        } else {
            return Result.failure(errors);
        }
    }

    private static Instant convertToUtcTime(Date date) {
        return ZonedDateTime.ofInstant(date.toInstant(), UTC).toInstant();
    }
}
