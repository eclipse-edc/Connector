/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.spi.policy;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Rule;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.function.BiFunction;

/**
 * Evaluates policies.
 */
public interface PolicyEngine {

    /**
     * Evaluates the given policy for an agent.
     */
    Result<Policy> evaluate(Policy policy, ParticipantAgent agent);

    /**
     * Registers a function that is invoked when a policy contains an atomic constraint whose left operator expression evaluates to the given key.
     *
     * @param type the function type
     * @param key the key
     * @param function the function
     */
    <R extends Rule> void registerFunction(Class<R> type, String key, AtomicConstraintFunction<R> function);

    /**
     * Registers a function that is invoked when a policy contains a rule of the given type.
     *
     * @param type the rule type
     * @param function the function
     */
    <R extends Rule> void registerFunction(Class<R> type, RuleFunction<R> function);

    /**
     * Registers a function that performs pre-validation on the policy.
     */
    void registerPreValidator(BiFunction<Policy, PolicyContext, Boolean> validator);

    /**
     * Registers a function that performs post-validation on the policy.
     */
    void registerPostValidator(BiFunction<Policy, PolicyContext, Boolean> validator);
}
