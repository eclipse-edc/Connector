/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.ids.spi.policy;

import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Prohibition;

/**
 * Evaluates a constraint attached to a prohibition.
 */
@FunctionalInterface
public interface IdsOfferProhibitionFunction {

    /**
     * Performs the evaluation.
     *
     * @param operator the operation
     * @param rightValue the right-side expression for the constraint
     * @param prohibition the prohibition associated with the constraint
     * @param context the policy context
     */
    boolean evaluate(Operator operator, String rightValue, Prohibition prohibition, IdsOfferPolicyContext context);

}
