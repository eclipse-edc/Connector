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

package org.eclipse.dataspaceconnector.policy.engine;

import org.eclipse.dataspaceconnector.policy.model.Rule;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

/**
 * An extension point that evaluates and modifies a {@link ResourceDefinition} according to a
 * {@link Rule}.
 *
 * @param <RULE_TYPE> the type of rule.
 * @param <DEFINITION_TYPE> the type of resource definition.
 */
@FunctionalInterface
public interface ResourceDefinitionRuleFunction<RULE_TYPE extends Rule, DEFINITION_TYPE extends ResourceDefinition> {
    
    /**
     * Evaluates and modifies the given {@link ResourceDefinition} so that the given rule is
     * fulfilled.
     *
     * @param rule the rule.
     * @param resourceDefinition the resource definition to evaluate.
     * @return a result containing either the verified/modified resource definition or the problems
     *         encountered during evaluation.
     */
    Result<DEFINITION_TYPE> evaluate(RULE_TYPE rule, DEFINITION_TYPE resourceDefinition);
    
}
