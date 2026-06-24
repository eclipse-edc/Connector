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

package org.eclipse.edc.policy.model;

/**
 * Invoked during policy evaluation as when the left operand of an atomic constraint evaluates to a key that is not bound to a {@link org.eclipse.edc.policy.model.AtomicConstraintFunction}.
 * The function is responsible for performing policy evaluation on the right operand and the left operand.
 */
@FunctionalInterface
public interface DynamicAtomicConstraintFunction<LEFT_VALUE, RIGHT_VALUE, RULE_TYPE extends Rule, RESULT> {

    /**
     * Performs the evaluation.
     *
     * @param leftValue  the left-side expression for the constraint
     * @param operator   the operation
     * @param rightValue the right-side expression for the constraint; the concrete type may be a string, primitive or object such as a JSON-LD encoded collection.
     * @param rule       the rule associated with the constraint
     * @return the result of the evaluation
     */
    RESULT evaluate(LEFT_VALUE leftValue, Operator operator, RIGHT_VALUE rightValue, RULE_TYPE rule);

}
