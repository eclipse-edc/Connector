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
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.PolicyRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyValidatorRule;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan;
import org.eclipse.edc.policy.engine.validation.PolicyValidator;
import org.eclipse.edc.policy.evaluator.PolicyEvaluator;
import org.eclipse.edc.policy.evaluator.RuleProblem;
import org.eclipse.edc.policy.model.AtomicConstraintFunction;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.DynamicAtomicConstraintFunction;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.policy.model.RuleFunction;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Default implementation of the policy engine.
 */
public class PolicyEngineImpl implements PolicyEngine {

    private final Map<String, Class<? extends PolicyContext>> scopes = new HashMap<>();

    private final List<ConstraintFunctionEntry<Rule, ? extends PolicyContext>> constraintFunctions = new ArrayList<>();
    private final List<DynamicConstraintFunctionEntry<Rule, ? extends PolicyContext>> dynamicConstraintFunctions = new ArrayList<>();
    private final List<RuleFunctionEntry<Rule, ? extends PolicyContext>> ruleFunctions = new ArrayList<>();

    private final List<ValidatorRuleEntry<? extends PolicyContext>> preValidators = new ArrayList<>();
    private final List<ValidatorRuleEntry<? extends PolicyContext>> postValidators = new ArrayList<>();

    private final ScopeFilter scopeFilter;
    private final RuleBindingRegistry ruleBindingRegistry;

    public PolicyEngineImpl(RuleBindingRegistry ruleBindingRegistry) {
        this.scopeFilter = new ScopeFilter(ruleBindingRegistry);
        this.ruleBindingRegistry = ruleBindingRegistry;
    }

    @Override
    public <C extends PolicyContext> Result<Void> evaluate(Policy policy, C context) {
        Predicate<FunctionEntry<?>> isScoped = entry -> entry.contextType().isAssignableFrom(context.getClass());

        var preValidationFailure = preValidators.stream()
                .filter(isScoped)
                .map(it -> (PolicyValidatorRule<C>) it.rule())
                .filter(it -> !it.apply(policy, context))
                .findFirst();

        if (preValidationFailure.isPresent()) {
            return failValidator("Pre-validator", preValidationFailure.get(), context);
        }

        var evalBuilder = PolicyEvaluator.Builder.newInstance();

        ruleFunctions.stream()
                .filter(isScoped)
                .forEach(entry -> {
                    if (Duty.class.isAssignableFrom(entry.type)) {
                        evalBuilder.dutyRuleFunction(ruleFunction(context, entry));
                    } else if (Permission.class.isAssignableFrom(entry.type)) {
                        evalBuilder.permissionRuleFunction(ruleFunction(context, entry));
                    } else if (Prohibition.class.isAssignableFrom(entry.type)) {
                        evalBuilder.prohibitionRuleFunction(ruleFunction(context, entry));
                    }
                });

        constraintFunctions.stream()
                .filter(isScoped)
                .forEach(entry -> {
                    if (Duty.class.isAssignableFrom(entry.type)) {
                        evalBuilder.dutyFunction(entry.key, constraintFunction(context, entry));
                    } else if (Permission.class.isAssignableFrom(entry.type)) {
                        evalBuilder.permissionFunction(entry.key, constraintFunction(context, entry));
                    } else if (Prohibition.class.isAssignableFrom(entry.type)) {
                        evalBuilder.prohibitionFunction(entry.key, constraintFunction(context, entry));
                    }
                });

        dynamicConstraintFunctions.stream()
                .filter(isScoped)
                .forEach(entry -> {
                    if (Duty.class.isAssignableFrom(entry.type)) {
                        evalBuilder.dynamicDutyFunction(entry.function::canHandle, dynamicConstraintFunction(context, entry));
                    } else if (Permission.class.isAssignableFrom(entry.type)) {
                        evalBuilder.dynamicPermissionFunction(entry.function::canHandle, dynamicConstraintFunction(context, entry));
                    } else if (Prohibition.class.isAssignableFrom(entry.type)) {
                        evalBuilder.dynamicProhibitionFunction(entry.function::canHandle, dynamicConstraintFunction(context, entry));
                    }
                });

        var evaluator = evalBuilder.build();

        var filteredPolicy = scopeFilter.applyScope(policy, context.scope());

        var result = evaluator.evaluate(filteredPolicy);
        if (!result.valid()) {
            return failure("Policy in scope %s not fulfilled: %s".formatted(context.scope(), result.getProblems().stream().map(RuleProblem::getDescription).toList()));
        }

        var postValidationFailure = postValidators.stream()
                .filter(isScoped)
                .map(it -> (PolicyValidatorRule<C>) it.rule())
                .filter(it -> !it.apply(policy, context))
                .findFirst();

        if (postValidationFailure.isPresent()) {
            return failValidator("Post-validator", postValidationFailure.get(), context);
        }

        return success();
    }

    @Override
    public Result<Void> validate(Policy policy) {
        var validatorBuilder = PolicyValidator.Builder.newInstance()
                .ruleBindingRegistry(ruleBindingRegistry);

        constraintFunctions.forEach(entry -> validatorBuilder.evaluationFunction(entry.key, entry.type, entry.function));
        dynamicConstraintFunctions.forEach(entry -> validatorBuilder.dynamicEvaluationFunction(entry.type, entry.function));

        return validatorBuilder.build().validate(policy);
    }

    @Override
    public PolicyEvaluationPlan createEvaluationPlan(String scope, Policy policy) {
        var planner = PolicyEvaluationPlanner.Builder.newInstance(scope).ruleBindingRegistry(ruleBindingRegistry);
        Predicate<FunctionEntry<?>> isScoped = entry -> entry.contextType().isAssignableFrom(contextType(scope));

        preValidators.stream().filter(isScoped).map(ValidatorRuleEntry::rule)
                .forEach(planner::preValidator);
        postValidators.stream().filter(isScoped).map(ValidatorRuleEntry::rule)
                .forEach(planner::postValidator);

        constraintFunctions.stream().filter(isScoped)
                .forEach(entry -> planner.evaluationFunction(entry.key, entry.type, entry.function));
        dynamicConstraintFunctions.stream().filter(isScoped)
                .forEach(entry -> planner.evaluationFunction(entry.type, entry.function));
        ruleFunctions.stream().filter(isScoped)
                .forEach(entry -> planner.evaluationFunction(entry.type, entry.function));

        return policy.accept(planner.build());
    }

    @Override
    public <C extends PolicyContext> void registerScope(String scope, Class<C> contextType) {
        scopes.put(scope, contextType);
    }

    @Override
    public <R extends Rule, C extends PolicyContext> void registerFunction(Class<C> contextType, Class<R> type, String key, AtomicConstraintRuleFunction<R, C> function) {
        constraintFunctions.add(new ConstraintFunctionEntry(contextType, type, key, function));
    }

    @Override
    public <R extends Rule, C extends PolicyContext> void registerFunction(Class<C> contextType, Class<R> type, DynamicAtomicConstraintRuleFunction<R, C> function) {
        dynamicConstraintFunctions.add(new DynamicConstraintFunctionEntry(contextType, type, function));
    }

    @Override
    public <R extends Rule, C extends PolicyContext> void registerFunction(Class<C> contextType, Class<R> type, PolicyRuleFunction<R, C> function) {
        ruleFunctions.add(new RuleFunctionEntry(contextType, type, function));
    }

    @Override
    public <C extends PolicyContext> void registerPreValidator(Class<C> contextType, PolicyValidatorRule<C> validator) {
        preValidators.add(new ValidatorRuleEntry(contextType, validator));
    }

    @Override
    public <C extends PolicyContext> void registerPostValidator(Class<C> contextType, PolicyValidatorRule<C> validator) {
        postValidators.add(new ValidatorRuleEntry(contextType, validator));
    }

    @Override
    public void bindScope(String ruleType, String scope) {
        ruleBindingRegistry.bind(ruleType, scope);
    }

    @Override
    public void dynamicScopeBinder(Function<String, Set<String>> binder) {
        ruleBindingRegistry.dynamicBind(binder);
    }

    @NotNull
    private Result<Void> failValidator(String type, PolicyValidatorRule<?> validator, PolicyContext context) {
        return failure(context.hasProblems() ? context.getProblems() : List.of(type + " failed: " + validator.name()));
    }

    private <C extends PolicyContext, R extends Rule> @NotNull DynamicAtomicConstraintFunction<Object, Object, R, Boolean> dynamicConstraintFunction(C context, DynamicConstraintFunctionEntry<Rule, ? extends PolicyContext> entry) {
        return (key, operator, value, duty) ->
                ((DynamicAtomicConstraintRuleFunction<R, C>) entry.function).evaluate(key, operator, value, duty, context);
    }

    private <C extends PolicyContext, R extends Rule> @NotNull AtomicConstraintFunction<Object, R, Boolean> constraintFunction(C context, ConstraintFunctionEntry<Rule, ? extends PolicyContext> entry) {
        return (operator, value, duty) ->
                ((AtomicConstraintRuleFunction<R, C>) entry.function).evaluate(operator, value, duty, context);
    }

    private <C extends PolicyContext, R extends Rule> @NotNull RuleFunction<R> ruleFunction(C context, RuleFunctionEntry<Rule, ? extends PolicyContext> entry) {
        return (rule) -> ((PolicyRuleFunction<R, C>) entry.function).evaluate(rule, context);
    }

    private record ConstraintFunctionEntry<R extends Rule, C extends PolicyContext>(
            Class<C> contextType,
            Class<R> type,
            String key,
            AtomicConstraintRuleFunction<R, C> function
    ) implements FunctionEntry<C>  { }

    private record DynamicConstraintFunctionEntry<R extends Rule, C extends PolicyContext>(
            Class<C> contextType,
            Class<R> type,
            DynamicAtomicConstraintRuleFunction<R, C> function
    ) implements FunctionEntry<C>  { }

    private record RuleFunctionEntry<R extends Rule, C extends PolicyContext>(
            Class<C> contextType,
            Class<R> type,
            PolicyRuleFunction<R, C> function
    ) implements FunctionEntry<C>  { }

    private record ValidatorRuleEntry<C extends PolicyContext>(
            Class<C> contextType,
            PolicyValidatorRule<C> rule
    ) implements FunctionEntry<C> { }

    private interface FunctionEntry<C extends PolicyContext> {
        Class<C> contextType();
    }

    private @NotNull Class<? extends PolicyContext> contextType(String scope) {
        var contextType = scopes.get(scope);
        if (contextType == null) {
            throw new EdcException("Scope %s not registered".formatted(scope));
        }
        return contextType;
    }

}
