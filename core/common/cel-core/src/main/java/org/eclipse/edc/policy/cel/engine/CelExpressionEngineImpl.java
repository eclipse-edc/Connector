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

package org.eclipse.edc.policy.cel.engine;

import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.internal.ProtoTimeUtils;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.cel.store.CelExpressionStore;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CelExpressionEngineImpl implements CelExpressionEngine {

    private final TransactionContext ctx;
    private final CelExpressionStore store;
    private final Monitor monitor;

    private final CelCompiler celCompiler = CelCompilerFactory.standardCelCompilerBuilder()
            .addVar("this", SimpleType.DYN)
            .addVar("ctx", SimpleType.DYN)
            .addVar("now", SimpleType.TIMESTAMP)
            .setStandardMacros(CelStandardMacro.STANDARD_MACROS)
            .setResultType(SimpleType.BOOL)
            .build();


    private final CelRuntime celRuntime = CelRuntimeFactory.standardCelRuntimeBuilder().build();

    public CelExpressionEngineImpl(TransactionContext ctx, CelExpressionStore store, Monitor monitor) {
        this.ctx = ctx;
        this.store = store;
        this.monitor = monitor;
    }

    @Override
    public ServiceResult<Void> validate(String expression) {
        return compile(expression)
                .flatMap(r -> ServiceResult.from(r.mapEmpty()));

    }

    @Override
    public boolean canEvaluate(String leftOperand) {
        return !fetch(leftOperand).isEmpty();
    }

    @Override
    public Set<String> evaluationScopes(String leftOperand) {
        return fetch(leftOperand)
                .stream()
                .flatMap(expr -> expr.getScopes().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public ServiceResult<Boolean> test(String expression, Object leftOperand, Operator operator, Object rightOperand, Map<String, Object> params) {
        return compile(expression)
                .compose(ast -> evaluateAst(ast, leftOperand, operator, rightOperand, params))
                .flatMap(ServiceResult::from);

    }

    @Override
    public ServiceResult<Boolean> evaluateExpression(Object leftOperand, Operator operator, Object rightOperand, Map<String, Object> params) {
        var compileResult = fetchAndCompile(leftOperand.toString());
        if (compileResult.failed()) {
            monitor.severe("Failed to compile expressions for left operand: " + leftOperand + ". Reason: " + compileResult.getFailureDetail());
            return ServiceResult.badRequest("Failed to compile expressions for left operand: " + leftOperand + ". Reason: " + compileResult.getFailureDetail());
        }
        var expressions = compileResult.getContent();
        if (expressions.isEmpty()) {
            monitor.severe("No expressions registered for left operand: " + leftOperand);
            return ServiceResult.badRequest("No expressions registered for left operand: " + leftOperand);
        }
        var result = true;
        for (var ast : expressions) {
            var evaluationResult = evaluateAst(ast, leftOperand, operator, rightOperand, params);

            if (evaluationResult.failed()) {
                monitor.severe("Failed to evaluate expression for left operand: " + leftOperand + ". Reason: " + evaluationResult.getFailureDetail());
                return ServiceResult.badRequest("Failed to evaluate expression for left operand: " + leftOperand + ". Reason: " + evaluationResult.getFailureDetail());
            }
            result = evaluationResult.getContent();
            if (!result) {
                break;
            }
        }
        return ServiceResult.success(result);
    }

    private Result<Boolean> evaluateAst(CelAbstractSyntaxTree ast, Object leftOperand, Operator operator, Object rightOperand, Map<String, Object> params) {
        try {
            var program = celRuntime.createProgram(ast);
            Map<String, Object> newParams = new HashMap<>();
            newParams.put("now", ProtoTimeUtils.now());
            newParams.put("this", Map.of("leftOperand", leftOperand, "operator", operator.name(), "rightOperand", rightOperand));
            newParams.put("ctx", params);

            return Result.success((Boolean) program.eval(newParams));
        } catch (CelEvaluationException e) {
            // Report any evaluation errors, if present
            return Result.failure("Evaluation error has occurred. Reason: " + e.getMessage());
        }
    }

    private Result<List<CelAbstractSyntaxTree>> fetchAndCompile(String leftOperand) {
        return fetch(leftOperand).stream()
                .map(expr -> compile(expr.getExpression()))
                .collect(Result.collector());
    }

    private List<CelExpression> fetch(String leftOperand) {
        return ctx.execute(() -> store.query(QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("leftOperand", "=", leftOperand))
                .build()));

    }

    private Result<CelAbstractSyntaxTree> compile(String expression) {
        CelAbstractSyntaxTree ast;
        try {
            // Parse the expression
            ast = celCompiler.parse(expression).getAst();
            // Type-check the expression for correctness
            ast = celCompiler.check(ast).getAst();
        } catch (CelValidationException e) {
            return Result.failure("Failed to validate expression. Reason: " + e.getMessage());
        }
        return Result.success(ast);
    }
}
