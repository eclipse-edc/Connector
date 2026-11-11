/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.verifiablecredentials.jwt.rules;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

import static org.eclipse.edc.verifiablecredentials.jwt.Constants.TYPE;

/**
 * Asserts that the {@code type} claim of a VCDM 2.0 presentation JWT is, or contains, the expected type.
 */
public class PresentationTypeRule implements TokenValidationRule {

    private final String expectedType;

    public PresentationTypeRule(String expectedType) {
        this.expectedType = expectedType;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var type = toVerify.getClaim(TYPE);
        if (type == null) {
            return Result.failure("Not a valid VP token - missing the '%s' claim".formatted(TYPE));
        }
        var matches = type instanceof Collection<?> types ? types.contains(expectedType) : expectedType.equals(type);
        if (!matches) {
            return Result.failure("Not a valid VP token - expected type '%s', but was '%s'".formatted(expectedType, type));
        }
        return Result.success();
    }
}
