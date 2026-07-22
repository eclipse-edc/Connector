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
import org.eclipse.edc.policy.cel.function.CelFunctionRegistry;
import org.eclipse.edc.policy.cel.function.CelFunctionTranslator;
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
import java.util.stream.Stream;

public class CelExpressionEngineImpl implements CelExpressionEngine {

    private final TransactionContext ctx;
    private final CelExpressionStore store;
    private final Monitor monitor;
    private final CelFunctionRegistry functionRegistry;

    // built lazily on first use: extensions register their custom functions during initialization, which happens
    // after this engine is constructed. Declarations and bindings are derived from a single snapshot of the registry,
    // so the compiler can never type-check against a function the runtime cannot dispatch.
    private volatile CelEnvironment environment;

    public CelExpressionEngineImpl(TransactionContext ctx, CelExpressionStore store, Monitor monitor, CelFunctionRegistry functionRegistry) {
        this.ctx = ctx;
        this.store = store;
        this.monitor = monitor;
        this.functionRegistry = functionRegistry;
    }

    private CelEnvironment environment() {
        var current = environment;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (environment == null) {
                var functions = functionRegistry.seal();
                environment = new CelEnvironment(
                        CelCompilerFactory.standardCelCompilerBuilder()
                                .addVar("this", SimpleType.DYN)
                                .addVar("ctx", SimpleType.DYN)
                                .addVar("now", SimpleType.TIMESTAMP)
                                .setStandardMacros(CelStandardMacro.STANDARD_MACROS)
                                .setResultType(SimpleType.BOOL)
                                .addFunctionDeclarations(CelFunctionTranslator.toDeclarations(functions))
                                .build(),
                        CelRuntimeFactory.standardCelRuntimeBuilder()
                                .addFunctionBindings(CelFunctionTranslator.toBindings(functions))
                                .build());
            }
            return environment;
        }
    }

    private record CelEnvironment(CelCompiler compiler, CelRuntime runtime) {
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
        return Stream.concat(fetch(leftOperand).stream(), fetchByAction(leftOperand).stream())
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
            var program = environment().runtime().createProgram(ast);
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

    private List<CelExpression> fetchByAction(String action) {
        return ctx.execute(() -> store.query(QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("actions", "contains", action))
                .build()));

    }

    private Result<CelAbstractSyntaxTree> compile(String expression) {
        CelAbstractSyntaxTree ast;
        try {
            // Parse the expression
            var compiler = environment().compiler();
            ast = compiler.parse(expression).getAst();
            // Type-check the expression for correctness
            ast = compiler.check(ast).getAst();
        } catch (CelValidationException e) {
            return Result.failure("Failed to validate expression. Reason: " + e.getMessage());
        }
        return Result.success(ast);
    }
}
