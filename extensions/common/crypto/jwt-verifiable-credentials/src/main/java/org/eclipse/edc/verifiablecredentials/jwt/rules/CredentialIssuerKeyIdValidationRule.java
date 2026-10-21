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
 * Asserts that the {@code kid} header of a VCDM 2.0 credential JWT belongs to the credential's {@code issuer}:
 * <pre>
 *     kid := issuer # key-id
 * </pre>
 * where {@code issuer} is either the {@code issuer} property, if it is a string, or its {@code id}, if it is an object.
 * If the JWT also has an {@code iss} claim, it must be equal to the issuer.
 */
public class CredentialIssuerKeyIdValidationRule implements TokenValidationRule {
    private static final String ISSUER_PROPERTY = "issuer";
    private static final String ID_PROPERTY = "id";

    private final String keyId;

    public CredentialIssuerKeyIdValidationRule(@Nullable String tokenKeyIdHeader) {
        this.keyId = tokenKeyIdHeader;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var issuer = issuerId(toVerify.getClaim(ISSUER_PROPERTY));
        if (issuer == null) {
            return Result.failure("Credential must have an '%s' property that is a string or an object with an '%s'.".formatted(ISSUER_PROPERTY, ID_PROPERTY));
        }

        var iss = toVerify.getStringClaim(JwtRegisteredClaimNames.ISSUER);
        if (iss != null && !iss.equals(issuer)) {
            return Result.failure("'iss' claim ('%s') expected to be equal to the credential's issuer ('%s'), but it was not.".formatted(iss, issuer));
        }

        if (keyId == null || !keyId.startsWith(issuer + "#")) {
            return Result.failure("kid header '%s' expected to correlate to the credential's issuer ('%s'), but it did not.".formatted(keyId, issuer));
        }
        return Result.success();
    }

    private @Nullable String issuerId(Object issuer) {
        if (issuer instanceof String issuerString) {
            return issuerString;
        }
        if (issuer instanceof Map<?, ?> issuerObject && issuerObject.get(ID_PROPERTY) instanceof String id) {
            return id;
        }
        return null;
    }
}
