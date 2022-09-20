/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.validation;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationRule;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.Date;
import java.util.Map;

import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.EXPIRATION_TIME;

/**
 * Assert that token containing these claims is not expired yet.
 */
public class ExpirationDateValidationRule implements TokenValidationRule {

    private final Clock clock;

    public ExpirationDateValidationRule(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var expiration = (Date) toVerify.getClaims().get(EXPIRATION_TIME);
        if (expiration == null) {
            return Result.failure("Missing expiration time in token");
        }

        // check contract expiration date
        if (clock.instant().isAfter(expiration.toInstant())) {
            return Result.failure("Token has expired on " + expiration);
        }

        return Result.success();
    }
}
