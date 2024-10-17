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

import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * This rule checks that the JTI claim is valid, that means that the same JTI claim has not been encountered within the token's lifetime.
 */
public class JtiValidationRule implements TokenValidationRule {

    private final JtiValidationStore jtiValidationStore;

    public JtiValidationRule(JtiValidationStore jtiValidationStore) {
        this.jtiValidationStore = jtiValidationStore;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var jti = toVerify.getStringClaim(JwtRegisteredClaimNames.JWT_ID);
        if (jti != null) {
            var entry = jtiValidationStore.findById(jti);
            return entry != null
                    ? Result.success()
                    : Result.failure("The JWT id '%s' was not found".formatted(jti));
        }
        return Result.success();
    }
}
