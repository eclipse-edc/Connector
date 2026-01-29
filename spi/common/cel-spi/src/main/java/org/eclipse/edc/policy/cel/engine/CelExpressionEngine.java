/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.policy.cel.engine;

import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Map;
import java.util.Set;


/**
 * Engine interface for evaluating CEL (Common Expression Language) expressions.
 */
public interface CelExpressionEngine {

    /**
     * Checks if the engine can evaluate the given left operand.
     *
     * @param leftOperand the left operand to check
     * @return true if the engine can evaluate the left operand, false otherwise
     */
    boolean canEvaluate(String leftOperand);

    /**
     * Retrieves the evaluation scopes for the given left operand.
     *
     * @param leftOperand the left operand
     * @return a set of evaluation scopes
     */
    Set<String> evaluationScopes(String leftOperand);

    /**
     * Validates the given CEL expression.
     *
     * @param expression the CEL expression to validate
     * @return a service result indicating success or failure
     */
    ServiceResult<Void> validate(String expression);

    /**
     * Tests the given expression with the provided operands and parameters.
     *
     * @param expression   the CEL expression to test
     * @param leftOperand  the left operand
     * @param operator     the operator
     * @param rightOperand the right operand
     * @param params       additional parameters for evaluation
     * @return a service result containing the boolean result of the test or an error
     */
    ServiceResult<Boolean> test(String expression, Object leftOperand, Operator operator, Object rightOperand, Map<String, Object> params);

    /**
     * Evaluates the expression based on the provided operands and parameters.
     *
     * @param leftOperand  the left operand
     * @param operator     the operator
     * @param rightOperand the right operand
     * @param params       additional parameters for evaluation
     * @return a service result containing the boolean result of the evaluation or an error
     */
    ServiceResult<Boolean> evaluateExpression(Object leftOperand, Operator operator, Object rightOperand, Map<String, Object> params);
}
