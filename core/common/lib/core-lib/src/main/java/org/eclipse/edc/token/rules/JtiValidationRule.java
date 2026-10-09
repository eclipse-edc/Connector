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

package org.eclipse.edc.token.rules;

import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.jwt.validation.jti.JtiValidationEntry;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreFailure;
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
        if (jti == null) {
            return Result.success();
        }

        // the entry expires with the token, which is rejected afterwards anyway. Storing it fails if it exists, so that a
        // token that is presented several times at once, e.g. to several replicas, is only accepted once
        var expiration = toVerify.getInstantClaim(JwtRegisteredClaimNames.EXPIRATION_TIME);
        var result = jtiValidationStore.storeEntry(new JtiValidationEntry(jti, expiration != null ? expiration.toEpochMilli() : null));
        if (result.succeeded()) {
            return Result.success();
        }
        if (result.reason() == StoreFailure.Reason.ALREADY_EXISTS) {
            return Result.failure("The JWT id '%s' was already used.".formatted(jti));
        }
        monitor.warning("Failed to store the JWT id '%s': %s".formatted(jti, result.getFailureDetail()));
        return Result.failure(result.getFailureDetail());
    }
}
