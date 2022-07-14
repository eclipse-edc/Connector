/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.transfer.provision.evaluation;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Rule;
import org.eclipse.dataspaceconnector.spi.policy.PolicyContext;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

/**
 * Evaluates resource manifest to ensure they comply to a given policy. During evaluation,
 * a resource manifest may also be modified.
 */
public interface ResourceManifestEvaluator {
    
    /**
     * Evaluates the resource manifest from the policy context, if it is present, for the given policy.
     * In order to comply with the policy, the manifest may be modified.
     *
     * @param policy the policy.
     * @param policyContext the policy context.
     * @return true, if the policy is fulfilled; false otherwise.
     */
    boolean evaluate(Policy policy, PolicyContext policyContext);
    
    /**
     * Registers a new rule function for evaluation of a specific resource definition type.
     * The function is invoked when a policy contains a rule of the given type.
     *
     * @param resourceType the resource definition type.
     * @param ruleType the rule type.
     * @param function the evaluation function.
     */
    <D extends ResourceDefinition, R extends Rule> void registerFunction(Class<D> resourceType, Class<R> ruleType, ResourceDefinitionRuleFunction<R, D> function);
    
    /**
     * Registers a new atomic constraint function for evaluation of a specific resource definition type.
     * The function is invoked when a policy contains an atomic constraint whose left operator expression evaluates
     * to the given key.
     *
     * @param key the key.
     * @param resourceType the resource definition type.
     * @param ruleType the rule type.
     * @param function the evaluation function.
     */
    <D extends ResourceDefinition, R extends Rule> void registerFunction(String key, Class<D> resourceType, Class<R> ruleType, ResourceDefinitionAtomicConstraintFunction<R, D> function);
    
}
