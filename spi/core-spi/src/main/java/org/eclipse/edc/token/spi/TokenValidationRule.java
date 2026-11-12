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

package org.eclipse.edc.token.spi;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Interface for token validation rules.
 */
@FunctionalInterface
public interface TokenValidationRule {

    /**
     * Returns a rule that applies all the given rules. Every rule is checked, the result contains the failures of all
     * the rules that failed.
     *
     * @param rules the rules to apply
     * @return the composite rule, that succeeds if there are no rules
     */
    static TokenValidationRule allOf(List<TokenValidationRule> rules) {
        return (toVerify, additional) -> rules.stream()
                .map(rule -> rule.checkRule(toVerify, additional))
                .reduce(Result.success(), Result::merge);
    }

    Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional);
}
