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

import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Rule;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.function.BiFunction;

/**
 * Evaluates policies.
 *
 * A policy scope is a visibility and semantic boundary for a {@link Rule}. A rule binding associates a rule type (see below) with a scope identified by a key, thereby
 * making a policy visible in a scope. Rule and constraint functions can be bound to one or more scopes, limiting the semantics they implement to the scope they are
 * registered with.
 *
 * A rule type has two manifestations: (1) The type of {@link Action} specified by a rule; or (2) The left-hand operand of an {@link AtomicConstraint} contained in the rule.
 *
 * Scopes are hierarchical and delimited by {@link #DELIMITER}. Functions bound to parent scopes will be inherited in child scopes.
 */
public interface PolicyEngine {

    /**
     * Wildcard denoting all scopes.
     */
    String ALL_SCOPES = "*";

    /**
     * Scope delimiter.
     */
    String DELIMITER = ".";

    /**
     * Evaluates the given policy for an agent for the given scope.
     */
    Result<Policy> evaluate(String scope, Policy policy, ParticipantAgent agent);

    /**
     * Registers a function that is invoked when a policy contains an atomic constraint whose left operator expression evaluates to the given key for the specified scope.
     *
     * @param scope the scope the function applies to
     * @param type the function type
     * @param key the key
     * @param function the function
     */
    <R extends Rule> void registerFunction(String scope, Class<R> type, String key, AtomicConstraintFunction<R> function);

    /**
     * Registers a function that is invoked when a policy contains a rule of the given type for the specified scope.
     *
     * @param scope the scope the function applies to
     * @param type the {@link Rule} sub-type
     * @param function the function
     */
    <R extends Rule> void registerFunction(String scope, Class<R> type, RuleFunction<R> function);

    /**
     * Registers a function that performs pre-validation on the policy for the given scope.
     */
    void registerPreValidator(String scope, BiFunction<Policy, PolicyContext, Boolean> validator);

    /**
     * Registers a function that performs post-validation on the policy for the given scope.
     */
    void registerPostValidator(String scope, BiFunction<Policy, PolicyContext, Boolean> validator);
}
