/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.policy.cel.function;

import org.eclipse.edc.policy.cel.engine.CelExpressionEngine;
import org.eclipse.edc.policy.cel.function.context.CelContextMapper;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.result.Result;

public record CelExpressionFunction<C extends PolicyContext, R extends Rule>(
        CelExpressionEngine engine,
        CelContextMapper<C> contextSupplier) implements DynamicAtomicConstraintRuleFunction<R, C> {


    @Override
    public boolean evaluate(Object leftOperand, Operator operator, Object rightOperand, R rule, C c) {
        var params = contextSupplier.mapContext(c);
        if (params.failed()) {
            c.reportProblem("Failed to obtain context for CEL evaluation: %s".formatted(params.getFailureMessages().toString()));
            return false;
        }
        var result = engine.evaluateExpression(leftOperand.toString(), operator, rightOperand, params.getContent());
        if (result.failed()) {
            c.reportProblem("CEL evaluation failed: %s".formatted(result.getFailureMessages().toString()));
            return false;
        }
        return result.getContent();
    }

    @Override
    public boolean canHandle(Object leftOperand) {
        return engine.canEvaluate(leftOperand.toString());
    }

    @Override
    public Result<Void> validate(Object leftOperand, Operator operator, Object rightOperand, R rule) {
        if (engine.canEvaluate(leftOperand.toString(), operator)) {
            return Result.success();
        }
        return Result.failure("No CEL expression registered for left operand '%s' supports operator '%s'".formatted(leftOperand, operator.name()));
    }

}
