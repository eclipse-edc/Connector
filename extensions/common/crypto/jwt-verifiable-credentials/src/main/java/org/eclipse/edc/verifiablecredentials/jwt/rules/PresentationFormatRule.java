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

import org.eclipse.edc.iam.decentralizedclaims.spi.verification.JwtPresentationFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static org.eclipse.edc.verifiablecredentials.jwt.Constants.VC_CLAIM;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.VP_CLAIM;

/**
 * Asserts that a JWT is a presentation of the expected format, as determined by {@link JwtPresentationFormat}.
 */
public class PresentationFormatRule implements TokenValidationRule {

    private final CredentialFormat expectedFormat;

    /**
     * Creates the rule for the expected format.
     *
     * @param expectedFormat either {@link CredentialFormat#VC1_0_JWT} or {@link CredentialFormat#VC2_0_JOSE}
     */
    public PresentationFormatRule(CredentialFormat expectedFormat) {
        if (expectedFormat != CredentialFormat.VC1_0_JWT && expectedFormat != CredentialFormat.VC2_0_JOSE) {
            throw new IllegalArgumentException("Unsupported presentation format: " + expectedFormat);
        }
        this.expectedFormat = expectedFormat;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        if (JwtPresentationFormat.of(toVerify.getClaims()) == expectedFormat) {
            return Result.success();
        }
        if (expectedFormat == CredentialFormat.VC1_0_JWT) {
            return Result.failure("Not a valid VCDM 1.1 presentation: the JWT must not contain the '%s' claim or a VCDM 2.0 '@context' besides the '%s' claim."
                    .formatted(VC_CLAIM, VP_CLAIM));
        }
        return Result.failure("Not a valid VCDM 2.0 presentation: the JWT must have the VCDM 2.0 '@context', and must not contain the '%s' or '%s' claim."
                .formatted(VP_CLAIM, VC_CLAIM));
    }
}
