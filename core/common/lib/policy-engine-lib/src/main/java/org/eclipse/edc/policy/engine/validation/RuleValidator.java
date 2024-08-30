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

package org.eclipse.edc.policy.engine.validation;

import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;

/**
 * Validates rules in a {@link RuleBindingRegistry}
 */
public class RuleValidator {

    private final RuleBindingRegistry registry;

    public RuleValidator(RuleBindingRegistry registry) {
        this.registry = registry;
    }

    /**
     * Checks if the input ruleType is bound to any scope
     */
    public boolean isBounded(String ruleType) {
        return !registry.bindings(ruleType).isEmpty();
    }

    /**
     * Checks if the input ruleType is bound within the input scope
     */
    public boolean isInScope(String ruleType, String scope) {
        return registry.isInScope(ruleType, scope);
    }
}
