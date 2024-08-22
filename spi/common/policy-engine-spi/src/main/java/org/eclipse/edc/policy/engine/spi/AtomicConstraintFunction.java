/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.policy.engine.spi;

import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.result.Result;

/**
 * Invoked during policy evaluation when the left operand of an atomic constraint evaluates to a key associated with this function. The function is responsible for performing
 * policy evaluation on the right operand.
 */
@FunctionalInterface
public interface AtomicConstraintFunction<R extends Rule> {

    /**
     * Performs the evaluation.
     *
     * @param operator   the operation
     * @param rightValue the right-side expression for the constraint; the concrete type may be a string, primitive or object such as a JSON-LD encoded collection.
     * @param rule       the rule associated with the constraint
     * @param context    the policy context
     */
    boolean evaluate(Operator operator, Object rightValue, R rule, PolicyContext context);


    /**
     * Performs a validation of an atomic constraint
     *
     * @param operator   the operation
     * @param rightValue the right-side expression for the constraint; the concrete type may be a string, primitive or object such as a JSON-LD encoded collection
     * @param rule       the rule associated with the constraint
     * @return the result of the validation
     */
    default Result<Void> validate(Operator operator, Object rightValue, R rule) {
        return Result.success();
    }
}
