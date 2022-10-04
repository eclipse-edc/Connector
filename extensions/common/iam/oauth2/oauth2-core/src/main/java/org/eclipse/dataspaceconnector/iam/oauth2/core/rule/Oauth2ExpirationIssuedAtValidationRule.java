/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.iam.oauth2.core.rule;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationRule;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.Map;

import static org.eclipse.dataspaceconnector.spi.jwt.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtRegisteredClaimNames.ISSUED_AT;

/**
 * Token validation rule that checks if the token is not expired and if the "issued at" claim is valued correctly
 */
public class Oauth2ExpirationIssuedAtValidationRule implements TokenValidationRule {

    private final Clock clock;

    public Oauth2ExpirationIssuedAtValidationRule(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var now = clock.instant();
        var expires = toVerify.getInstantClaim(EXPIRATION_TIME);
        if (expires == null) {
            return Result.failure("Required expiration time (exp) claim is missing in token");
        } else if (now.isAfter(expires)) {
            return Result.failure("Token has expired (exp)");
        }

        var issuedAt = toVerify.getInstantClaim(ISSUED_AT);
        if (issuedAt != null) {
            if (issuedAt.isAfter(expires)) {
                return Result.failure("Issued at (iat) claim is after expiration time (exp) claim in token");
            } else if (now.isBefore(issuedAt)) {
                return Result.failure("Current date/time before issued at (iat) claim in token");
            }
        }

        return Result.success();
    }

}