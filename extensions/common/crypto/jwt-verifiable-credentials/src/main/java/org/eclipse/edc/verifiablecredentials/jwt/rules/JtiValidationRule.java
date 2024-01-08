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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * This rule checks that the JTI claim is valid, that means that the same JTI claim has not been encountered within the token's lifetime.
 * <p>
 * Note that this rule can only be implemented after <a href="https://github.com/eclipse-edc/Connector/issues/3749">this related issue</a>
 */
public class JtiValidationRule implements TokenValidationRule {
    private final Monitor monitor;

    public JtiValidationRule(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        monitor.warning("The JTI Validation rule is not yet implemented as it depends on https://github.com/eclipse-edc/Connector/issues/3749.");
        return Result.success();
    }
}
