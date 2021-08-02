/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.spi.policy;

import com.microsoft.dagx.policy.model.Operator;
import com.microsoft.dagx.policy.model.Permission;

/**
 * Evaluates a constraint attached to a permission.
 */
@FunctionalInterface
public interface IdsOfferPermissionFunction {

    /**
     * Performs the evaluation.
     *
     * @param operator the operation
     * @param rightValue the right-side expression for the constraint
     * @param permission the permission associated with the constraint
     * @param context the policy context
     */
    boolean evaluate(Operator operator, String rightValue, Permission permission, IdsOfferPolicyContext context);
}
