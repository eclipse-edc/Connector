/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.ids.spi.policy;

import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;

/**
 ** Evaluates a constraint attached to a permission.
 */
@FunctionalInterface
public interface IdsRequestPermissionFunction {

    /**
     * Performs the evaluation.
     *
     * @param operator the operation
     * @param rightValue the right-side expression for the constraint
     * @param permission the permission associated with the constraint
     * @param context the policy context
     */
    boolean evaluate(Operator operator, String rightValue, Permission permission, IdsRequestPolicyContext context);
}
