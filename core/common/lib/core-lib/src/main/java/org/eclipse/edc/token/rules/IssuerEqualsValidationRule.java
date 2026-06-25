/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.token.rules;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public record IssuerEqualsValidationRule(String expectedIssuer) implements TokenValidationRule {

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var iss = toVerify.getStringClaim("iss");
        return Objects.equals(iss, expectedIssuer) ?
                Result.success() :
                Result.failure("'iss' claim does not match expected issuer, expected '%s', got '%s'".formatted(expectedIssuer, iss));
    }
}
