/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.policy.engine.spi;

import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.result.Result;

/**
 * Invoked during policy evaluation as when the left operand of an atomic constraint evaluates to a key that is not bound to a {@link AtomicConstraintFunction}.
 * The function is responsible for performing policy evaluation on the right operand and the left operand.
 */
public interface DynamicAtomicConstraintFunction<R extends Rule> {

    /**
     * Performs the evaluation.
     *
     * @param leftValue  the left-side expression for the constraint
     * @param operator   the operation
     * @param rightValue the right-side expression for the constraint; the concrete type may be a string, primitive or object such as a JSON-LD encoded collection.
     * @param rule       the rule associated with the constraint
     * @param context    the policy context
     */
    boolean evaluate(Object leftValue, Operator operator, Object rightValue, R rule, PolicyContext context);

    /**
     * Returns true if the function can evaluate the input left operand.
     *
     * @param leftValue the left-side expression for the constraint
     * @return true if the function can evaluate the left operand, false otherwise
     */
    boolean canHandle(Object leftValue);

    /**
     * Performs a validation of an atomic constraint
     *
     * @param leftValue  the left-side expression for the constraint
     * @param operator   the operation
     * @param rightValue the right-side expression for the constraint; the concrete type may be a string, primitive or object such as a JSON-LD encoded collection
     * @param rule       the rule associated with the constraint
     * @return the result of the validation
     */
    default Result<Void> validate(Object leftValue, Operator operator, Object rightValue, R rule) {
        return Result.success();
    }

    /**
     * Returns the name of the function
     */
    default String name() {
        return getClass().getSimpleName();
    }

}
