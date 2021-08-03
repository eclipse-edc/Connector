/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.ids.spi.policy;

import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Operator;

/**
 * Evaluates a constraint attached to a duty.
 */
@FunctionalInterface
public interface IdsRequestDutyFunction {

    /**
     * Performs the evaluation.
     *
     * @param operator the operation
     * @param rightValue the right-side expression for the constraint
     * @param duty the duty associated with the constraint
     * @param context the policy context
     */
    boolean evaluate(Operator operator, String rightValue, Duty duty, IdsRequestPolicyContext context);

}
