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

package org.eclipse.edc.policy.engine.spi;

import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

import java.util.function.BiFunction;

/**
 * Evaluates policies.
 * <p>
 * A policy scope is a visibility and semantic boundary for a {@link Rule}. A rule binding associates a rule type (see below) with a scope identified by a key, thereby
 * making a policy visible in a scope. Rule and constraint functions can be bound to one or more scopes, limiting the semantics they implement to the scope they are
 * registered with.
 * <p>
 * A rule type has two manifestations: (1) The type of {@link Action} specified by a rule; or (2) The left-hand operand of an {@link AtomicConstraint} contained in the rule.
 * <p>
 * Scopes are hierarchical and delimited by {@link #DELIMITER}. Functions bound to parent scopes will be inherited in child scopes.
 */
@ExtensionPoint
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
     * Returns a filtered policy for the scope. This involves recursively removing rules and constraints not bound to the scope and returning a modified copy of the unfiltered
     * policy.
     */
    Policy filter(Policy policy, String scope);

    /**
     * Evaluates the given policy with a context for the given scope.
     */
    Result<Void> evaluate(String scope, Policy policy, PolicyContext context);

    /**
     * Validates the given policy.
     */
    Result<Void> validate(Policy policy);

    /**
     * Registers a function that is invoked when a policy contains an atomic constraint whose left operator expression evaluates to the given key for the specified scope.
     *
     * @param scope    the scope the function applies to
     * @param type     the function type
     * @param key      the key
     * @param function the function
     */
    <R extends Rule> void registerFunction(String scope, Class<R> type, String key, AtomicConstraintFunction<R> function);

    /**
     * Registers a function that is invoked when a policy contains an atomic constraint whose left operator expression evaluates to the given key that's not bound
     * to an {@link AtomicConstraintFunction}.
     *
     * @param scope    the scope the function applies to
     * @param type     the function type
     * @param function the function
     */
    <R extends Rule> void registerFunction(String scope, Class<R> type, DynamicAtomicConstraintFunction<R> function);

    /**
     * Registers a function that is invoked when a policy contains a rule of the given type for the specified scope.
     *
     * @param scope    the scope the function applies to
     * @param type     the {@link Rule} subtype
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
