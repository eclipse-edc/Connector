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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * This rule checks that the JTI claim is valid, that means that the same JTI claim has not been encountered within the token's lifetime.
 * <p>
 */
public class JtiValidationRule implements TokenValidationRule {

    private final JtiValidationStore jtiValidationStore;
    private final Monitor monitor;

    public JtiValidationRule(JtiValidationStore jtiValidationStore, Monitor monitor) {
        this.jtiValidationStore = jtiValidationStore;
        this.monitor = monitor;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var jti = toVerify.getStringClaim(JwtRegisteredClaimNames.JWT_ID);
        if (jti != null) {
            var entry = jtiValidationStore.findById(jti);
            if (entry == null) {
                return Result.failure("The JWT id '%s' was not found".formatted(jti));
            }
            if (entry.isExpired()) {
                monitor.warning("JTI Validation entry with id " + jti + " is expired");
            }
        }
        return Result.success();
    }
}
