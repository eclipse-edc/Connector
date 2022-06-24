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

package org.eclipse.dataspaceconnector.spi.policy.evaluation;

import org.eclipse.dataspaceconnector.policy.model.Rule;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

/**
 * Invoked during policy evaluation to verify, and, if required, modify a {@link ResourceDefinition}
 * to fulfil a rule.
 *
 * @param <R> the type of rule.
 * @param <D> the type of resource definiton.
 */
public interface ResourceDefinitionRuleFunction<R extends Rule, D extends ResourceDefinition> {
    
    /**
     * Verifies and modifies the resource definition so that the rule is fulfilled.
     *
     * @param rule the rule.
     * @param resourceDefinition the resource definition.
     * @return a result containing either the verified/modified resource definition or the problems
     *         encountered during evaluation.
     */
    Result<ResourceDefinition> evaluate(R rule, D resourceDefinition);
    
}
