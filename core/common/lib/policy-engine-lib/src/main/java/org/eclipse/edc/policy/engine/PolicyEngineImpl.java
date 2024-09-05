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

package org.eclipse.edc.policy.engine;

import org.eclipse.edc.policy.engine.plan.PolicyEvaluationPlanner;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleFunction;
import org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan;
import org.eclipse.edc.policy.engine.validation.PolicyChainValidator;
import org.eclipse.edc.policy.engine.validation.PolicyPostValidator;
import org.eclipse.edc.policy.engine.validation.PolicyPostValidatorImpl;
import org.eclipse.edc.policy.engine.validation.PolicyPreValidator;
import org.eclipse.edc.policy.engine.validation.PolicyPreValidatorImpl;
import org.eclipse.edc.policy.engine.validation.PolicyValidator;
import org.eclipse.edc.policy.engine.validation.RuleValidator;
import org.eclipse.edc.policy.evaluator.PolicyEvaluator;
import org.eclipse.edc.policy.evaluator.RuleProblem;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Default implementation of the policy engine.
 */
public class PolicyEngineImpl implements PolicyEngine {

    public static final String ALL_SCOPES_DELIMITED = ALL_SCOPES + DELIMITER;

    private final Map<String, List<ConstraintFunctionEntry<Rule>>> constraintFunctions = new TreeMap<>();

    private final List<DynamicConstraintFunctionEntry<Rule>> dynamicConstraintFunctions = new ArrayList<>();

    private final Map<String, List<RuleFunctionEntry<Rule>>> ruleFunctions = new TreeMap<>();
    private final Map<String, List<PolicyPreValidator>> preValidators = new HashMap<>();
    private final Map<String, List<PolicyPostValidator>> postValidators = new HashMap<>();
    private final ScopeFilter scopeFilter;
    private final RuleValidator ruleValidator;

    public PolicyEngineImpl(ScopeFilter scopeFilter, RuleValidator ruleValidator) {
        this.scopeFilter = scopeFilter;
        this.ruleValidator = ruleValidator;
    }

    public static boolean scopeFilter(String entry, String scope) {
        return ALL_SCOPES_DELIMITED.equals(entry) || scope.startsWith(entry);
    }

    @Override
    public Policy filter(Policy policy, String scope) {
        return scopeFilter.applyScope(policy, scope);
    }

    @Override
    public Result<Void> evaluate(String scope, Policy policy, PolicyContext context) {
        var delimitedScope = scope + ".";

        var scopedPreValidators = preValidators.entrySet().stream().filter(entry -> scopeFilter(entry.getKey(), delimitedScope)).flatMap(l -> l.getValue().stream()).toList();
        for (var validator : scopedPreValidators) {
            if (!validator.apply(policy, context)) {
                return failValidator("Pre-validator", validator, context);
            }
        }

        var evalBuilder = PolicyEvaluator.Builder.newInstance();

        ruleFunctions.entrySet().stream().filter(entry -> scopeFilter(entry.getKey(), delimitedScope)).flatMap(entry -> entry.getValue().stream()).forEach(entry -> {
            if (Duty.class.isAssignableFrom(entry.type)) {
                evalBuilder.dutyRuleFunction((rule) -> entry.function.evaluate(rule, context));
            } else if (Permission.class.isAssignableFrom(entry.type)) {
                evalBuilder.permissionRuleFunction((rule) -> entry.function.evaluate(rule, context));
            } else if (Prohibition.class.isAssignableFrom(entry.type)) {
                evalBuilder.prohibitionRuleFunction((rule) -> entry.function.evaluate(rule, context));
            }
        });

        constraintFunctions.entrySet().stream().filter(entry -> scopeFilter(entry.getKey(), delimitedScope)).flatMap(entry -> entry.getValue().stream()).forEach(entry -> {
            if (Duty.class.isAssignableFrom(entry.type)) {
                evalBuilder.dutyFunction(entry.key, (operator, value, duty) -> entry.function.evaluate(operator, value, duty, context));
            } else if (Permission.class.isAssignableFrom(entry.type)) {
                evalBuilder.permissionFunction(entry.key, (operator, value, permission) -> entry.function.evaluate(operator, value, permission, context));
            } else if (Prohibition.class.isAssignableFrom(entry.type)) {
                evalBuilder.prohibitionFunction(entry.key, (operator, value, prohibition) -> entry.function.evaluate(operator, value, prohibition, context));
            }
        });

        dynamicConstraintFunctions.stream().filter(entry -> scopeFilter(entry.scope, delimitedScope)).forEach(entry -> {
            if (Duty.class.isAssignableFrom(entry.type)) {
                evalBuilder.dynamicDutyFunction(entry.function::canHandle, (key, operator, value, duty) -> entry.function.evaluate(key, operator, value, duty, context));
            } else if (Permission.class.isAssignableFrom(entry.type)) {
                evalBuilder.dynamicPermissionFunction(entry.function::canHandle, (key, operator, value, permission) -> entry.function.evaluate(key, operator, value, permission, context));
            } else if (Prohibition.class.isAssignableFrom(entry.type)) {
                evalBuilder.dynamicProhibitionFunction(entry.function::canHandle, (key, operator, value, prohibition) -> entry.function.evaluate(key, operator, value, prohibition, context));
            }
        });

        var evaluator = evalBuilder.build();

        var filteredPolicy = scopeFilter.applyScope(policy, scope);

        var result = evaluator.evaluate(filteredPolicy);

        if (result.valid()) {

            var scopedPostValidators = postValidators.entrySet().stream().filter(entry -> scopeFilter(entry.getKey(), delimitedScope)).flatMap(l -> l.getValue().stream()).toList();
            for (var validator : scopedPostValidators) {
                if (!validator.apply(policy, context)) {
                    return failValidator("Post-validator", validator, context);
                }
            }

            return success();
        } else {
            return failure(result.getProblems().stream().map(RuleProblem::getDescription).collect(toList()));
        }
    }

    @Override
    public Result<Void> validate(Policy policy) {
        var validatorBuilder = PolicyValidator.Builder.newInstance()
                .ruleValidator(ruleValidator);

        constraintFunctions.values().stream()
                .flatMap(Collection::stream)
                .forEach(entry -> validatorBuilder.evaluationFunction(entry.key, entry.type, entry.function));

        dynamicConstraintFunctions.forEach(entry -> validatorBuilder.dynamicEvaluationFunction(entry.type, entry.function));

        return validatorBuilder.build().validate(policy);
    }

    @Override
    public PolicyEvaluationPlan createEvaluationPlan(String scope, Policy policy) {
        var delimitedScope = scope + DELIMITER;
        var planner = PolicyEvaluationPlanner.Builder.newInstance(delimitedScope).ruleValidator(ruleValidator);

        preValidators.forEach(planner::preValidators);
        postValidators.forEach(planner::postValidators);

        constraintFunctions.forEach((functionScope, entry) -> entry.forEach(constraintEntry -> {
            planner.evaluationFunction(functionScope, constraintEntry.key, constraintEntry.type, constraintEntry.function);
        }));

        dynamicConstraintFunctions.forEach(dynFunctions ->
                planner.evaluationFunction(dynFunctions.scope, dynFunctions.type, dynFunctions.function)
        );

        ruleFunctions.forEach((functionScope, entry) -> entry.forEach(functionEntry -> {
            planner.evaluationFunction(functionScope, functionEntry.type, functionEntry.function);
        }));

        return policy.accept(planner.build());
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <R extends Rule> void registerFunction(String scope, Class<R> type, String key, AtomicConstraintFunction<R> function) {
        constraintFunctions.computeIfAbsent(scope + ".", k -> new ArrayList<>()).add(new ConstraintFunctionEntry(type, key, function));
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <R extends Rule> void registerFunction(String scope, Class<R> type, DynamicAtomicConstraintFunction<R> function) {
        dynamicConstraintFunctions.add(new DynamicConstraintFunctionEntry(type, scope + DELIMITER, function));
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <R extends Rule> void registerFunction(String scope, Class<R> type, RuleFunction<R> function) {
        ruleFunctions.computeIfAbsent(scope + ".", k -> new ArrayList<>()).add(new RuleFunctionEntry(type, function));
    }

    @Override
    public void registerPreValidator(String scope, BiFunction<Policy, PolicyContext, Boolean> validator) {
        preValidators.computeIfAbsent(scope + DELIMITER, k -> new ArrayList<>()).add(new PolicyPreValidatorImpl(validator));
    }

    @Override
    public void registerPostValidator(String scope, BiFunction<Policy, PolicyContext, Boolean> validator) {
        postValidators.computeIfAbsent(scope + DELIMITER, k -> new ArrayList<>()).add(new PolicyPostValidatorImpl(validator));
    }

    @NotNull
    private Result<Void> failValidator(String type, PolicyChainValidator validator, PolicyContext context) {
        return failure(context.hasProblems() ? context.getProblems() : List.of(type + " failed: " + validator.getClass().getName()));
    }

    private static class ConstraintFunctionEntry<R extends Rule> {
        Class<R> type;
        String key;
        AtomicConstraintFunction<R> function;

        ConstraintFunctionEntry(Class<R> type, String key, AtomicConstraintFunction<R> function) {
            this.type = type;
            this.key = key;
            this.function = function;
        }
    }

    private static class DynamicConstraintFunctionEntry<R extends Rule> {
        Class<R> type;
        String scope;
        DynamicAtomicConstraintFunction<R> function;

        DynamicConstraintFunctionEntry(Class<R> type, String scope, DynamicAtomicConstraintFunction<R> function) {
            this.type = type;
            this.scope = scope;
            this.function = function;
        }
    }

    private static class RuleFunctionEntry<R extends Rule> {
        Class<R> type;
        RuleFunction<R> function;

        RuleFunctionEntry(Class<R> type, RuleFunction<R> function) {
            this.type = type;
            this.function = function;
        }
    }

}
