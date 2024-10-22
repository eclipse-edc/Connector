/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.token.rules;

import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.Map;


/**
 * Token validation rule that checks if the "not before" claim is valid
 */
public class NotBeforeValidationRule implements TokenValidationRule {
    private final Clock clock;
    private final int notBeforeLeeway;
    private final boolean allowNull;

    /**
     * Instantiates the rule
     *
     * @deprecated Please use {@link NotBeforeValidationRule#NotBeforeValidationRule(Clock, int, boolean)} instead.
     */
    @Deprecated(since = "0.7.0")
    public NotBeforeValidationRule(Clock clock, int notBeforeLeeway) {
        this(clock, notBeforeLeeway, false);
    }

    public NotBeforeValidationRule(Clock clock, int notBeforeLeeway, boolean allowNull) {
        this.clock = clock;
        this.notBeforeLeeway = notBeforeLeeway;
        this.allowNull = allowNull;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var now = clock.instant();
        var leewayNow = now.plusSeconds(notBeforeLeeway);
        var notBefore = toVerify.getInstantClaim(JwtRegisteredClaimNames.NOT_BEFORE);

        if (notBefore == null) {
            if (!allowNull) {
                return Result.failure("Required not before (nbf) claim is missing in token");
            }
        } else if (leewayNow.isBefore(notBefore)) {
            return Result.failure("Current date/time with leeway before the not before (nbf) claim in token");
        }

        return Result.success();
    }
}
