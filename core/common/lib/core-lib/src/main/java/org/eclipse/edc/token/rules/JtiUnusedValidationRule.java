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

package org.eclipse.edc.token.rules;

import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Checks that the JTI claim has not been used yet, without recording it.
 * <p>
 * This is meant for a verification that has more steps after the token validation, and that records the JTI with a
 * {@link JtiValidationRule} only once all the steps succeeded. A token whose verification could not be completed can
 * then be presented again, while a token that was already accepted is rejected before the remaining steps run.
 */
public class JtiUnusedValidationRule implements TokenValidationRule {

    private final JtiValidationStore jtiValidationStore;
    private final Monitor monitor;

    public JtiUnusedValidationRule(JtiValidationStore jtiValidationStore, Monitor monitor) {
        this.jtiValidationStore = jtiValidationStore;
        this.monitor = monitor;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var jti = toVerify.getStringClaim(JwtRegisteredClaimNames.JWT_ID);
        if (jti == null) {
            return Result.success();
        }

        try {
            // the entry must not be removed by the lookup
            var entry = jtiValidationStore.findById(jti, false);
            if (entry != null && !entry.isExpired()) {
                return Result.failure("The JWT id '%s' was already used.".formatted(jti));
            }
            return Result.success();
        } catch (EdcPersistenceException e) {
            monitor.warning("Failed to look up the JWT id '%s': %s".formatted(jti, e.getMessage()));
            // the token could not be checked, which says nothing about the token itself
            return Result.retryableFailure(e.getMessage());
        }
    }
}
