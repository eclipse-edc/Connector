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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ENVELOPED_VERIFIABLE_PRESENTATION_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.VERIFIABLE_PRESENTATION_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.VP_CLAIM;

/**
 * Asserts that a JWT, which is expected to be a credential, is not a presentation: it must neither have a {@code vp} claim
 * (VCDM 1.1) nor a presentation type (VCDM 2.0).
 */
public class NotPresentationRule implements TokenValidationRule {

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        if (toVerify.getClaims().containsKey(VP_CLAIM) || hasPresentationType(toVerify.getClaim(TYPE))) {
            return Result.failure("Expected a credential, but the JWT contains a presentation.");
        }
        return Result.success();
    }

    private boolean hasPresentationType(@Nullable Object type) {
        var types = type instanceof Collection<?> collection ? collection : List.of(String.valueOf(type));
        return Stream.of(VERIFIABLE_PRESENTATION_TYPE, ENVELOPED_VERIFIABLE_PRESENTATION_TYPE).anyMatch(types::contains);
    }
}
