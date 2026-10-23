/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.verifiablecredentials.jwt.rules;

import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Asserts that a key ID belongs to the {@code holder} of a VCDM 2.0 presentation JWT:
 * <pre>
 *     kid := holder # key-id
 * </pre>
 * where {@code holder} is either the {@code holder} property, if it is a string, or its {@code id}, if it is an object.
 * If the JWT also has an {@code iss} claim, it must be equal to the holder.
 */
public class PresentationHolderKeyIdValidationRule implements TokenValidationRule {
    private static final String HOLDER_PROPERTY = "holder";
    private static final String ID_PROPERTY = "id";

    private final String keyId;

    /**
     * Creates the rule for the key ID that signed the presentation, or that signed the envelope it is contained in.
     */
    public PresentationHolderKeyIdValidationRule(@Nullable String keyId) {
        this.keyId = keyId;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var holder = holderId(toVerify.getClaim(HOLDER_PROPERTY));
        if (holder == null) {
            return Result.failure("Presentation must have a '%s' property that is a string or an object with an '%s'.".formatted(HOLDER_PROPERTY, ID_PROPERTY));
        }

        var iss = toVerify.getStringClaim(JwtRegisteredClaimNames.ISSUER);
        if (iss != null && !iss.equals(holder)) {
            return Result.failure("'iss' claim ('%s') expected to be equal to the presentation's holder ('%s'), but it was not.".formatted(iss, holder));
        }

        if (keyId == null || !keyId.startsWith(holder + "#")) {
            return Result.failure("kid header '%s' expected to correlate to the presentation's holder ('%s'), but it did not.".formatted(keyId, holder));
        }
        return Result.success();
    }

    private @Nullable String holderId(Object holder) {
        if (holder instanceof String holderString) {
            return holderString;
        }
        if (holder instanceof Map<?, ?> holderObject && holderObject.get(ID_PROPERTY) instanceof String id) {
            return id;
        }
        return null;
    }
}
