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

package org.eclipse.edc.policy.engine.spi.plan.step;

import org.eclipse.edc.policy.engine.spi.PolicyRuleFunction;
import org.eclipse.edc.policy.model.Rule;

/**
 * An evaluation step for {@link PolicyRuleFunction} associated to a {@link Rule}
 */
public record RuleFunctionStep<R extends Rule>(PolicyRuleFunction<R, ?> function, R rule) {

    /**
     * Returns the {@link PolicyRuleFunction#name()}
     */
    public String functionName() {
        return function.name();
    }
}
