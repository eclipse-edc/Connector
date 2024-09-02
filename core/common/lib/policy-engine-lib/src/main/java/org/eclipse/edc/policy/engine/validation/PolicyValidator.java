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

package org.eclipse.edc.policy.engine.validation;

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.eclipse.edc.spi.result.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validate a policy.
 * <p>
 * The policy validator is used to validate policies against a set of configured rule bindings, {@link AtomicConstraintFunction} and {@link DynamicAtomicConstraintFunction}
 * <p>
 * The validation will fail under the following conditions:
 *
 * <ul>
 *     <li>If a rule action is not bound to a scope.</li>
 *     <li>If an {@link AtomicConstraint}'s left-operand is not bound to a scope.
 *     <li>If an {@link AtomicConstraint}'s left-operand is not bound to a function.
 * </ul>
 */
public class PolicyValidator implements Policy.Visitor<Result<Void>>, Rule.Visitor<Result<Void>>, Constraint.Visitor<Result<Void>> {

    private final Stack<Rule> ruleContext = new Stack<>();

    private final Map<String, List<ConstraintFunctionEntry<Rule>>> constraintFunctions = new TreeMap<>();
    private final List<DynamicAtomicConstraintFunctionEntry<Rule>> dynamicConstraintFunctions = new ArrayList<>();
    private RuleValidator ruleValidator;

    public Result<Void> validate(Policy policy) {
        return policy.accept(this);
    }

    @Override
    public Result<Void> visitAndConstraint(AndConstraint constraint) {
        return validateMultiplicityConstraint(constraint);
    }

    @Override
    public Result<Void> visitOrConstraint(OrConstraint constraint) {
        return validateMultiplicityConstraint(constraint);
    }

    @Override
    public Result<Void> visitXoneConstraint(XoneConstraint constraint) {
        return validateMultiplicityConstraint(constraint);
    }

    @Override
    public Result<Void> visitAtomicConstraint(AtomicConstraint constraint) {
        var currentRule = currentRule();
        var leftValue = constraint.getLeftExpression().accept(s -> s.getValue().toString());
        var rightValue = constraint.getRightExpression().accept(LiteralExpression::getValue);

        return validateLeftExpression(currentRule, leftValue)
                .merge(validateConstraint(leftValue, constraint.getOperator(), rightValue, currentRule));
    }

    @Override
    public Result<Void> visitPolicy(Policy policy) {
        return Stream.of(policy.getPermissions(), policy.getProhibitions(), policy.getObligations())
                .flatMap(Collection::stream)
                .map(rule -> rule.accept(this))
                .reduce(Result.success(), Result::merge);
    }

    @Override
    public Result<Void> visitPermission(Permission policy) {
        var result = policy.getDuties().stream()
                .map(duty -> duty.accept(this))
                .reduce(Result.success(), Result::merge);
        return result.merge(validateRule(policy));
    }

    @Override
    public Result<Void> visitProhibition(Prohibition prohibition) {
        return validateRule(prohibition);
    }

    @Override
    public Result<Void> visitDuty(Duty duty) {
        return validateRule(duty);
    }

    private Result<Void> validateMultiplicityConstraint(MultiplicityConstraint multiplicityConstraint) {
        return multiplicityConstraint.getConstraints().stream()
                .map(c -> c.accept(this))
                .reduce(Result.success(), Result::merge);
    }

    private Result<Void> validateLeftExpression(Rule rule, String leftOperand) {
        if (!ruleValidator.isBounded(leftOperand)) {
            return Result.failure("leftOperand '%s' is not bound to any scopes: Rule { %s } ".formatted(leftOperand, rule));
        } else {
            return Result.success();
        }
    }

    private Result<Void> validateConstraint(String leftOperand, Operator operator, Object rightOperand, Rule rule) {
        var functions = getFunctions(leftOperand, rule.getClass());
        if (functions.isEmpty()) {
            return Result.failure("left operand '%s' is not bound to any functions: Rule { %s }".formatted(leftOperand, rule));
        } else {
            return functions.stream()
                    .map(f -> f.validate(operator, rightOperand, rule))
                    .reduce(Result.success(), Result::merge);
        }
    }

    private Result<Void> validateRule(Rule rule) {
        var initialResult = validateAction(rule);
        try {
            ruleContext.push(rule);
            return rule.getConstraints().stream()
                    .map(constraint -> constraint.accept(this))
                    .reduce(initialResult, Result::merge);

        } finally {
            ruleContext.pop();
        }
    }

    private Result<Void> validateAction(Rule rule) {
        if (rule.getAction() != null && !ruleValidator.isBounded(rule.getAction().getType())) {
            return Result.failure("action '%s' is not bound to any scopes: Rule { %s }".formatted(rule.getAction().getType(), rule));
        } else {
            return Result.success();
        }
    }

    private <R extends Rule> List<AtomicConstraintFunction<Rule>> getFunctions(String key, Class<R> ruleKind) {
        // first look-up for an exact match
        var functions = constraintFunctions.getOrDefault(key, new ArrayList<>())
                .stream()
                .filter(entry -> ruleKind.isAssignableFrom(entry.type()))
                .map(entry -> entry.function)
                .collect(Collectors.toList());

        // if not found inspect the dynamic functions
        if (functions.isEmpty()) {
            functions = dynamicConstraintFunctions
                    .stream()
                    .filter(f -> ruleKind.isAssignableFrom(f.type))
                    .filter(f -> f.function.canHandle(key))
                    .map(entry -> wrapDynamicFunction(key, entry.function))
                    .toList();
        }

        return functions;
    }

    private <R extends Rule> AtomicConstraintFunction<R> wrapDynamicFunction(String key, DynamicAtomicConstraintFunction<R> function) {
        return new AtomicConstraintFunctionWrapper<>(key, function);
    }

    private Rule currentRule() {
        return ruleContext.peek();
    }

    public static class Builder {
        private final PolicyValidator validator;

        private Builder() {
            validator = new PolicyValidator();
        }

        public static PolicyValidator.Builder newInstance() {
            return new PolicyValidator.Builder();
        }

        public Builder ruleValidator(RuleValidator ruleValidator) {
            validator.ruleValidator = ruleValidator;
            return this;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <R extends Rule> Builder evaluationFunction(String key, Class<R> ruleKind, AtomicConstraintFunction<R> function) {
            validator.constraintFunctions.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new ConstraintFunctionEntry(ruleKind, function));
            return this;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <R extends Rule> Builder dynamicEvaluationFunction(Class<R> ruleKind, DynamicAtomicConstraintFunction<R> function) {
            validator.dynamicConstraintFunctions.add(new DynamicAtomicConstraintFunctionEntry(ruleKind, function));
            return this;
        }

        public PolicyValidator build() {
            Objects.requireNonNull(validator.ruleValidator, "Rule validator should not be null");
            return validator;
        }

    }

    private record ConstraintFunctionEntry<R extends Rule>(
            Class<R> type,
            AtomicConstraintFunction<R> function) {
    }

    private record DynamicAtomicConstraintFunctionEntry<R extends Rule>(
            Class<R> type,
            DynamicAtomicConstraintFunction<R> function) {
    }

    private record AtomicConstraintFunctionWrapper<R extends Rule>(
            String leftOperand,
            DynamicAtomicConstraintFunction<R> inner) implements AtomicConstraintFunction<R> {

        @Override
        public boolean evaluate(Operator operator, Object rightValue, R rule, PolicyContext context) {
            throw new UnsupportedOperationException("Evaluation is not supported");
        }

        @Override
        public Result<Void> validate(Operator operator, Object rightValue, R rule) {
            return inner.validate(leftOperand, operator, rightValue, rule);
        }
    }
}
