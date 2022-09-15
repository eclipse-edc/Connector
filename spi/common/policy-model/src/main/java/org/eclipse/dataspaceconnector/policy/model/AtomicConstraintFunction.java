/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.policy.model;

/**
 * Invoked during policy evaluation when the left operand of an atomic constraint evaluates to a key associated with this function.
 * The function is responsible for performing policy evaluation on the right operand.
 */
@FunctionalInterface
public interface AtomicConstraintFunction<RIGHT_VALUE, RULE_TYPE extends Rule, RESULT> {

    /**
     * Performs the evaluation.
     *
     * @param operator   the operation
     * @param rightValue the right-side expression for the constraint
     */
    RESULT evaluate(Operator operator, RIGHT_VALUE rightValue, RULE_TYPE rule);

}
