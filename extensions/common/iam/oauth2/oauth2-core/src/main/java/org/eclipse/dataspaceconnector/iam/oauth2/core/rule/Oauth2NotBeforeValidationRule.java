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

import static org.eclipse.dataspaceconnector.spi.jwt.JwtRegisteredClaimNames.NOT_BEFORE;

/**
 * Token validation rule that checks if the "not before" claim is valid
 */
public class Oauth2NotBeforeValidationRule implements TokenValidationRule {
    private final Clock clock;
    private final int notBeforeLeeway;

    public Oauth2NotBeforeValidationRule(Clock clock, int notBeforeLeeway) {
        this.clock = clock;
        this.notBeforeLeeway = notBeforeLeeway;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var now = clock.instant();
        var leewayNow = now.plusSeconds(notBeforeLeeway);
        var notBefore = toVerify.getInstantClaim(NOT_BEFORE);

        if (notBefore == null) {
            return Result.failure("Required not before (nbf) claim is missing in token");
        } else if (leewayNow.isBefore(notBefore)) {
            return Result.failure("Current date/time with leeway before the not before (nbf) claim in token");
        }

        return Result.success();
    }
}