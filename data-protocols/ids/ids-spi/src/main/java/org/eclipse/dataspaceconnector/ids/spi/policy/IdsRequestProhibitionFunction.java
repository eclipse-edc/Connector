/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.ids.spi.policy;

import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;

/**
 * Evaluates a constraint attached to a prohibition.
 */
@FunctionalInterface
public interface IdsRequestProhibitionFunction {

    /**
     * Performs the evaluation.
     *
     * @param operator the operation
     * @param rightValue the right-side expression for the constraint
     * @param prohibition the prohibition associated with the constraint
     * @param context the policy context
     */
    boolean evaluate(Operator operator, String rightValue, Prohibition prohibition, IdsRequestPolicyContext context);

}
