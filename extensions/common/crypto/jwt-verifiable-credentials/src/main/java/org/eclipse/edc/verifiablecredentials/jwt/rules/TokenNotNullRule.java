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

package org.eclipse.edc.verifiablecredentials.jwt.rules;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.util.string.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static org.eclipse.edc.iam.decentralizedclaims.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;

/**
 * Verifies that the "token" claim is present (= non-empty)
 */
public class TokenNotNullRule implements TokenValidationRule {

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        return StringUtils.isNullOrEmpty(toVerify.getStringClaim(PRESENTATION_TOKEN_CLAIM)) ?
                Result.failure("The '%s' claim is mandatory and must not be null.".formatted(PRESENTATION_TOKEN_CLAIM)) :
                Result.success();
    }
}
