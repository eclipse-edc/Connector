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

import java.util.ArrayList;
import java.util.Map;
import java.util.function.BiFunction;

import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ENVELOPED_PRESENTATION_CONTENT_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ID;

/**
 * Validates the presentations of an "Enveloped Verifiable Presentation", which is a compact form of enclosing VPs in a JWT.
 * The {@code id} claim must be a semicolon separated list of JWTs, prefixed with {@code data:application/vp+jwt,}, where
 * each one contains a single Verifiable Presentation, for example:
 * <pre>data:application/vp+jwt,eyAB...;data:application/vp+jwt,eyXYZ...</pre>
 * Every presentation JWT is verified with the given function, the rule succeeds only if all of them are valid.
 */
public class EnvelopedPresentationsRule implements TokenValidationRule {

    private final BiFunction<String, ClaimToken, Result<Void>> presentationVerifier;

    /**
     * Creates the rule.
     *
     * @param presentationVerifier verifies a single presentation JWT. It receives the presentation JWT and the envelope
     *                             token, e.g. to check that the key that signed the envelope belongs to the presentation's holder.
     */
    public EnvelopedPresentationsRule(BiFunction<String, ClaimToken, Result<Void>> presentationVerifier) {
        this.presentationVerifier = presentationVerifier;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        if (!(toVerify.getClaim(ID) instanceof String presentationEnvelope)) {
            return Result.failure("No enveloped presentation found in '%s' claim".formatted(ID));
        }

        var results = new ArrayList<Result<Void>>();
        for (var envelope : presentationEnvelope.split(";")) {
            if (!envelope.startsWith(ENVELOPED_PRESENTATION_CONTENT_TYPE + ",")) {
                return Result.failure("Incorrect presentation envelope. Must start with '%s,' but it did not.".formatted(ENVELOPED_PRESENTATION_CONTENT_TYPE));
            }
            results.add(presentationVerifier.apply(envelope.substring(ENVELOPED_PRESENTATION_CONTENT_TYPE.length() + 1), toVerify));
        }
        return results.stream().reduce(Result.success(), Result::merge);
    }
}
