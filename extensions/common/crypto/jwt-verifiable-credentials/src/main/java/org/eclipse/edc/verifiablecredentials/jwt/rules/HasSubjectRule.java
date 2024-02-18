/*
 *  Copyright (c) 2024 Amadeus
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

package org.eclipse.edc.verifiablecredentials.jwt.rules;

import com.nimbusds.jwt.JWTClaimNames;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.util.string.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Checks that the 'sub' claim contains a non-empty value.
 */
public class HasSubjectRule implements TokenValidationRule {

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        return StringUtils.isNullOrEmpty(toVerify.getStringClaim(JWTClaimNames.SUBJECT)) ?
                Result.failure("The '%s' claim is mandatory and must not be null.".formatted(JWTClaimNames.SUBJECT)) :
                Result.success();
    }
}
