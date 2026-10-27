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
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CelExpressionEngineImpl implements CelExpressionEngine {

    private final TransactionContext ctx;
    private final CelExpressionStore store;
    private final Monitor monitor;
    private final CelFunctionRegistry functionRegistry;
    private final Clock clock;
    private final long expressionsTtlMillis;

    // built lazily on first use: extensions register their custom functions during initialization, which happens
    // after this engine is constructed. Declarations and bindings are derived from a single snapshot of the registry,
    // so the compiler can never type-check against a function the runtime cannot dispatch.
    private volatile CelEnvironment environment;

    // programs of the registered expressions, by expression source. Parsing and type-checking an expression costs far
    // more than evaluating it, and the environment never changes once built, so each expression is compiled only once.
    // Arbitrary expressions passed to validate() and test() are not cached.
    private final Map<String, Result<CelRuntime.Program>> programs = new ConcurrentHashMap<>();

    // the registered expressions, indexed for the lookups done on every constraint evaluation. They are read from the
    // store at most once per TTL, and again after invalidate(), so changes made on other replicas show up within the TTL
    private final Object expressionsLock = new Object();
    private volatile Expressions expressions;

    /**
     * Creates an engine that reads the registered expressions from the store on every lookup.
     */
    public CelExpressionEngineImpl(TransactionContext ctx, CelExpressionStore store, Monitor monitor, CelFunctionRegistry functionRegistry) {
        this(ctx, store, monitor, functionRegistry, Clock.systemUTC(), 0);
    }

    /**
     * Creates an engine that keeps the registered expressions in memory for {@code expressionsTtlMillis}; 0 or less
     * reads them from the store on every lookup.
     */
    public CelExpressionEngineImpl(TransactionContext ctx, CelExpressionStore store, Monitor monitor, CelFunctionRegistry functionRegistry,
                                   Clock clock, long expressionsTtlMillis) {
        this.ctx = ctx;
        this.store = store;
        this.monitor = monitor;
        this.functionRegistry = functionRegistry;
        this.clock = clock;
        this.expressionsTtlMillis = expressionsTtlMillis;
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
    public boolean canEvaluate(String leftOperand, Operator operator) {
        return fetch(leftOperand).stream().anyMatch(expr -> expr.supportsOperator(operator));
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
        var registered = fetch(leftOperand.toString());
        if (registered.isEmpty()) {
            monitor.severe("No expressions registered for left operand: " + leftOperand);
            return ServiceResult.badRequest("No expressions registered for left operand: " + leftOperand);
        }
        var supported = registered.stream().filter(expr -> expr.supportsOperator(operator)).toList();
        if (supported.isEmpty()) {
            monitor.severe("No expressions registered for left operand: " + leftOperand + " supporting operator: " + operator.name());
            return ServiceResult.badRequest("No expressions registered for left operand: " + leftOperand + " supporting operator: " + operator.name());
        }
        var compileResult = supported.stream()
                .map(expr -> programs.computeIfAbsent(expr.getExpression(), this::program))
                .collect(Result.collector());
        if (compileResult.failed()) {
            monitor.severe("Failed to compile expressions for left operand: " + leftOperand + ". Reason: " + compileResult.getFailureDetail());
            return ServiceResult.badRequest("Failed to compile expressions for left operand: " + leftOperand + ". Reason: " + compileResult.getFailureDetail());
        }
        var result = true;
        for (var program : compileResult.getContent()) {
            var evaluationResult = evaluate(program, leftOperand, operator, rightOperand, params);

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
        return createProgram(ast).compose(program -> evaluate(program, leftOperand, operator, rightOperand, params));
    }

    private Result<CelRuntime.Program> program(String expression) {
        return compile(expression).compose(this::createProgram);
    }

    private Result<CelRuntime.Program> createProgram(CelAbstractSyntaxTree ast) {
        try {
            return Result.success(environment().runtime().createProgram(ast));
        } catch (CelEvaluationException e) {
            return Result.failure("Failed to create program. Reason: " + e.getMessage());
        }
    }

    private Result<Boolean> evaluate(CelRuntime.Program program, Object leftOperand, Operator operator, Object rightOperand, Map<String, Object> params) {
        try {
            Map<String, Object> newParams = new HashMap<>();
            newParams.put("now", ProtoTimeUtils.now());
            newParams.put("this", Map.of("leftOperand", leftOperand, "operator", operator.name(), "odrlOperator", odrlOperator(operator), "rightOperand", rightOperand));
            newParams.put("ctx", params);

            return Result.success((Boolean) program.eval(newParams));
        } catch (CelEvaluationException e) {
            // Report any evaluation errors, if present
            return Result.failure("Evaluation error has occurred. Reason: " + e.getMessage());
        }
    }

    /**
     * The ODRL operator name without the namespace (e.g. "eq", "isPartOf"), so that the deprecated {@link Operator#IN}
     * and {@link Operator#IS_PART_OF} are indistinguishable in expressions.
     */
    private String odrlOperator(Operator operator) {
        var iri = operator.getOdrlRepresentation();
        return iri.substring(iri.lastIndexOf('/') + 1);
    }

    @Override
    public void invalidate() {
        synchronized (expressionsLock) {
            expressions = null;
        }
    }

    private List<CelExpression> fetch(String leftOperand) {
        return expressions().byLeftOperand().getOrDefault(leftOperand, List.of());
    }

    private List<CelExpression> fetchByAction(String action) {
        return expressions().byAction().getOrDefault(action, List.of());
    }

    private Expressions expressions() {
        if (expressionsTtlMillis <= 0) {
            return loadExpressions();
        }
        var current = expressions;
        if (current != null && clock.instant().isBefore(current.expiresAt())) {
            return current;
        }
        synchronized (expressionsLock) {
            current = expressions;
            if (current == null || !clock.instant().isBefore(current.expiresAt())) {
                current = loadExpressions();
                expressions = current;
            }
            return current;
        }
    }

    private Expressions loadExpressions() {
        var all = ctx.execute(() -> store.query(QuerySpec.max()));
        var byLeftOperand = new HashMap<String, List<CelExpression>>();
        var byAction = new HashMap<String, List<CelExpression>>();
        for (var expression : all) {
            if (expression.getLeftOperand() != null) {
                byLeftOperand.computeIfAbsent(expression.getLeftOperand(), k -> new ArrayList<>()).add(expression);
            }
            for (var action : expression.getActions()) {
                byAction.computeIfAbsent(action, k -> new ArrayList<>()).add(expression);
            }
        }
        return new Expressions(byLeftOperand, byAction, clock.instant().plusMillis(expressionsTtlMillis));
    }

    private record Expressions(Map<String, List<CelExpression>> byLeftOperand, Map<String, List<CelExpression>> byAction,
                               Instant expiresAt) {
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
