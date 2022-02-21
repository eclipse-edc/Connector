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

package org.eclipse.dataspaceconnector.transfer.sync.api.rules;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.token.JwtClaimValidationRule;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Date;

/**
 * Assert that token containing these claims is not expired yet.
 */
public class ExpirationDateValidationRule implements JwtClaimValidationRule {

    @Override
    public Result<JWTClaimsSet> checkRule(@NotNull JWTClaimsSet toVerify) {
        Date expiration = toVerify.getExpirationTime();
        if (expiration == null) {
            return Result.failure("Missing expiration time in token");
        }

        // check contract expiration date
        if (Instant.now().isAfter(expiration.toInstant())) {
            return Result.failure("Token has expired");
        }

        return Result.success(toVerify);
    }
}
