/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.spi.policy;

import com.microsoft.dagx.policy.model.Duty;
import com.microsoft.dagx.policy.model.Operator;

/**
 * Evaluates a constraint attached to a duty.
 */
@FunctionalInterface
public interface IdsOfferDutyFunction {

    /**
     * Performs the evaluation.
     *
     * @param operator the operation
     * @param rightValue the right-side expression for the constraint
     * @param duty the duty associated with the constraint
     * @param context the policy context
     */
    boolean evaluate(Operator operator, String rightValue, Duty duty, IdsOfferPolicyContext context);

}
