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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Asserts that the {@code kid} header of a JWT contains the {@code iss}:
 * <pre>
 *     kid := iss # key-id
 * </pre>
 * where {@code iss} usually are DIDs, and the {@code key-id} is an arbitrary string.
 */
public class IssuerKeyIdValidationRule implements TokenValidationRule {
    private final String keyId;

    public IssuerKeyIdValidationRule(String tokenKeyIdHeader) {
        this.keyId = tokenKeyIdHeader;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var iss = toVerify.getStringClaim("iss");
        // keyID MUST be a composite of the issuer and the key-id in the form <ISSUER>#<keyID>
        return keyId.matches("%s#.*".formatted(iss)) ? Result.success() : Result.failure("kid header '%s' expected to correlate to 'iss' claim ('%s'), but it did not.".formatted(keyId, iss));
    }
}
