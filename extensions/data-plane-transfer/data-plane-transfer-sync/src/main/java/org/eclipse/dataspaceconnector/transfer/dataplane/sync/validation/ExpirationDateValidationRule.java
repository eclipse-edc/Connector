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

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.common.token.TokenValidationRule;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.time.Clock;
import java.util.Date;
import java.util.Map;

/**
 * Assert that token containing these claims is not expired yet.
 */
public class ExpirationDateValidationRule implements TokenValidationRule {

    private final Clock clock;

    public ExpirationDateValidationRule(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Result<SignedJWT> checkRule(@NotNull SignedJWT toVerify, @Nullable Map<String, Object> additional) {
        try {
            Date expiration = toVerify.getJWTClaimsSet().getExpirationTime();
            if (expiration == null) {
                return Result.failure("Missing expiration time in token");
            }

            // check contract expiration date
            if (clock.instant().isAfter(expiration.toInstant())) {
                return Result.failure("Token has expired");
            }

            return Result.success(toVerify);
        } catch (ParseException exception) {
            return Result.failure("Token could not be decoded");
        }
    }
}
