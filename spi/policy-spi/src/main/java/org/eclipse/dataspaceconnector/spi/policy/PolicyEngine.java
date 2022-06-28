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
 *       Fraunhofer Institute for Software and Systems Engineering - resource manifest evaluation
 *
 */

package org.eclipse.dataspaceconnector.spi.policy;

import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Rule;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.policy.evaluation.AtomicConstraintFunction;
import org.eclipse.dataspaceconnector.spi.policy.evaluation.ResourceDefinitionAtomicConstraintFunction;
import org.eclipse.dataspaceconnector.spi.policy.evaluation.ResourceDefinitionRuleFunction;
import org.eclipse.dataspaceconnector.spi.policy.evaluation.RuleFunction;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;

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
     * Returns a filtered policy for the scope. This involves recursively removing rules and constraints not bound to the scope and returning a modified copy of the unfiltered
     * policy.
     */
    Policy filter(Policy policy, String scope);

    /**
     * Evaluates the given policy for an agent for the given scope.
     */
    Result<Policy> evaluate(String scope, Policy policy, ParticipantAgent agent);
    
    /**
     * Evaluates and, if required, modifies the given ResourceManifest so that the given policy
     * is fulfilled.
     *
     * @param scope the scope.
     * @param policy the policy.
     * @param resourceManifest the resource manifest to evaluate.
     * @return a result containing either the verified/modified resource manifest or the problems
     *         encountered during evaluation.
     */
    Result<ResourceManifest> evaluate(String scope, Policy policy, ResourceManifest resourceManifest);

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
     * Registers a function that is invoked during resource manifest evaluation when a policy contains
     * an atomic constraint whose left operator expression evaluates to the given key for the specified scope.
     *
     * @param scope the scope the function applies to.
     * @param ruleType the {@link Rule} sub-type
     * @param resourceDefinitionType the {@link ResourceDefinition} sub-type
     * @param key the key.
     * @param function the function.
     */
    <R extends Rule, D extends ResourceDefinition> void registerFunction(String scope, Class<R> ruleType, Class<D> resourceDefinitionType, String key, ResourceDefinitionAtomicConstraintFunction<R, D> function);
    
    /**
     * Registers a function that is invoked during resource manifest evaluation when a policy contains a rule of the given type for the specified scope.
     *
     * @param scope the scope the function applies to.
     * @param ruleType the {@link Rule} sub-type
     * @param resourceDefinitionType the {@link ResourceDefinition} sub-type
     * @param function the function
     */
    <R extends Rule, D extends ResourceDefinition> void registerFunction(String scope, Class<R> ruleType, Class<D> resourceDefinitionType, ResourceDefinitionRuleFunction<R, D> function);

    /**
     * Registers a function that performs pre-validation on the policy for the given scope.
     */
    void registerPreValidator(String scope, BiFunction<Policy, PolicyContext, Boolean> validator);

    /**
     * Registers a function that performs post-validation on the policy for the given scope.
     */
    void registerPostValidator(String scope, BiFunction<Policy, PolicyContext, Boolean> validator);
}
