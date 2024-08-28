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

package org.eclipse.edc.policy.engine.plan;

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.RuleFunction;
import org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan;
import org.eclipse.edc.policy.engine.spi.plan.step.AndConstraintStep;
import org.eclipse.edc.policy.engine.spi.plan.step.AtomicConstraintStep;
import org.eclipse.edc.policy.engine.spi.plan.step.ConstraintStep;
import org.eclipse.edc.policy.engine.spi.plan.step.DutyStep;
import org.eclipse.edc.policy.engine.spi.plan.step.OrConstraintStep;
import org.eclipse.edc.policy.engine.spi.plan.step.PermissionStep;
import org.eclipse.edc.policy.engine.spi.plan.step.ProhibitionStep;
import org.eclipse.edc.policy.engine.spi.plan.step.RuleFunctionStep;
import org.eclipse.edc.policy.engine.spi.plan.step.RuleStep;
import org.eclipse.edc.policy.engine.spi.plan.step.ValidatorStep;
import org.eclipse.edc.policy.engine.spi.plan.step.XoneConstraintStep;
import org.eclipse.edc.policy.engine.validation.RuleValidator;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.eclipse.edc.policy.engine.PolicyEngineImpl.ALL_SCOPES_DELIMITED;

public class PolicyEvaluationPlanner implements Policy.Visitor<PolicyEvaluationPlan.Builder>, Rule.Visitor<RuleStep<? extends Rule>>, Constraint.Visitor<ConstraintStep> {

    private final Stack<Rule> ruleContext = new Stack<>();
    private final List<BiFunction<Policy, PolicyContext, Boolean>> preValidators = new ArrayList<>();
    private final List<BiFunction<Policy, PolicyContext, Boolean>> postValidators = new ArrayList<>();
    private final Map<String, List<ConstraintFunctionEntry<Rule>>> constraintFunctions = new TreeMap<>();
    private final List<DynamicAtomicConstraintFunctionEntry<Rule>> dynamicConstraintFunctions = new ArrayList<>();
    private final List<RuleFunctionFunctionEntry<Rule>> ruleFunctions = new ArrayList<>();
    private final String delimitedScope;

    private RuleValidator ruleValidator;

    private PolicyEvaluationPlanner(String delimitedScope) {
        this.delimitedScope = delimitedScope;
    }

    public PolicyEvaluationPlan evaluationPlan(Policy policy) {

        var planBuilder = policy.accept(this);

        preValidators.stream().map(ValidatorStep::new).forEach(planBuilder::preValidator);
        postValidators.stream().map(ValidatorStep::new).forEach(planBuilder::postValidator);

        return planBuilder.build();
    }

    @Override
    public AndConstraintStep visitAndConstraint(AndConstraint constraint) {
        var steps = validateMultiplicityConstraint(constraint);
        return new AndConstraintStep(steps, constraint);
    }

    @Override
    public OrConstraintStep visitOrConstraint(OrConstraint constraint) {
        var steps = validateMultiplicityConstraint(constraint);
        return new OrConstraintStep(steps, constraint);

    }

    @Override
    public XoneConstraintStep visitXoneConstraint(XoneConstraint constraint) {
        var steps = validateMultiplicityConstraint(constraint);
        return new XoneConstraintStep(steps, constraint);
    }

    @Override
    public AtomicConstraintStep visitAtomicConstraint(AtomicConstraint constraint) {
        var currentRule = currentRule();
        var leftValue = constraint.getLeftExpression().accept(s -> s.getValue().toString());
        var function = getFunctions(leftValue, currentRule.getClass());
        var isFiltered = !ruleValidator.isInScope(leftValue, delimitedScope) || function == null;

        return new AtomicConstraintStep(constraint, isFiltered, currentRule, function);
    }

    @Override
    public PolicyEvaluationPlan.Builder visitPolicy(Policy policy) {

        var builder = PolicyEvaluationPlan.Builder.newInstance();

        policy.getPermissions().stream().map(permission -> permission.accept(this))
                .map(PermissionStep.class::cast)
                .forEach(builder::permission);

        policy.getObligations().stream().map(obligation -> obligation.accept(this))
                .map(DutyStep.class::cast)
                .forEach(builder::obligation);

        policy.getProhibitions().stream().map(permission -> permission.accept(this))
                .map(ProhibitionStep.class::cast)
                .forEach(builder::prohibition);

        return builder;
    }

    @Override
    public PermissionStep visitPermission(Permission permission) {
        var permissionStepBuilder = PermissionStep.Builder.newInstance();
        visitRule(permission, permissionStepBuilder);

        permission.getDuties().stream().map(this::visitDuty)
                .forEach(permissionStepBuilder::dutyStep);

        return permissionStepBuilder.build();
    }

    @Override
    public ProhibitionStep visitProhibition(Prohibition prohibition) {
        var prohibitionStepBuilder = ProhibitionStep.Builder.newInstance();
        visitRule(prohibition, prohibitionStepBuilder);
        return prohibitionStepBuilder.build();
    }

    @Override
    public DutyStep visitDuty(Duty duty) {
        var prohibitionStepBuilder = DutyStep.Builder.newInstance();
        visitRule(duty, prohibitionStepBuilder);
        return prohibitionStepBuilder.build();
    }

    private AtomicConstraintFunction<Rule> getFunctions(String key, Class<? extends Rule> ruleKind) {
        return constraintFunctions.getOrDefault(key, new ArrayList<>())
                .stream()
                .filter(entry -> ruleKind.isAssignableFrom(entry.type()))
                .map(entry -> entry.function)
                .findFirst()
                .or(() -> dynamicConstraintFunctions
                        .stream()
                        .filter(f -> ruleKind.isAssignableFrom(f.type))
                        .filter(f -> f.function.canHandle(key))
                        .map(entry -> wrapDynamicFunction(key, entry.function))
                        .findFirst())
                .orElse(null);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <R extends Rule> void visitRule(R rule, RuleStep.Builder builder) {

        try {
            ruleContext.push(rule);
            builder.filtered(shouldIgnoreRule(rule));
            builder.rule(rule);

            for (var functionEntry : ruleFunctions) {
                if (rule.getClass().isAssignableFrom(functionEntry.type)) {
                    builder.ruleFunction(new RuleFunctionStep(functionEntry.function, rule));
                }
            }

            rule.getConstraints().stream()
                    .map(constraint -> constraint.accept(this))
                    .forEach(builder::constraint);

        } finally {
            ruleContext.pop();
        }

    }

    private Rule currentRule() {
        return ruleContext.peek();
    }

    private boolean shouldIgnoreRule(Rule rule) {
        return rule.getAction() != null && !ruleValidator.isBounded(rule.getAction().getType());
    }

    private List<ConstraintStep> validateMultiplicityConstraint(MultiplicityConstraint multiplicityConstraint) {
        return multiplicityConstraint.getConstraints()
                .stream()
                .map(c -> c.accept(this))
                .collect(Collectors.toList());
    }

    private <R extends Rule> AtomicConstraintFunction<R> wrapDynamicFunction(String key, DynamicAtomicConstraintFunction<R> function) {
        return new AtomicConstraintFunctionWrapper<>(key, function);
    }

    private record ConstraintFunctionEntry<R extends Rule>(
            Class<R> type,
            AtomicConstraintFunction<R> function) {
    }

    private record DynamicAtomicConstraintFunctionEntry<R extends Rule>(
            Class<R> type,
            DynamicAtomicConstraintFunction<R> function) {
    }

    private record RuleFunctionFunctionEntry<R extends Rule>(
            Class<R> type,
            RuleFunction<R> function) {
    }

    private record AtomicConstraintFunctionWrapper<R extends Rule>(
            String leftOperand,
            DynamicAtomicConstraintFunction<R> inner) implements AtomicConstraintFunction<R> {

        @Override
        public boolean evaluate(Operator operator, Object rightValue, R rule, PolicyContext context) {
            return inner.evaluate(leftOperand, operator, rightValue, rule, context);
        }

        @Override
        public Result<Void> validate(Operator operator, Object rightValue, R rule) {
            return inner.validate(leftOperand, operator, rightValue, rule);
        }
    }

    public static class Builder {
        private final PolicyEvaluationPlanner planner;

        private Builder(String scope) {
            planner = new PolicyEvaluationPlanner(scope);
        }

        public static PolicyEvaluationPlanner.Builder newInstance(String scope) {
            return new PolicyEvaluationPlanner.Builder(scope);
        }

        public Builder ruleValidator(RuleValidator ruleValidator) {
            planner.ruleValidator = ruleValidator;
            return this;
        }

        public Builder preValidator(String scope, BiFunction<Policy, PolicyContext, Boolean> validator) {

            if (scopeFilter(scope, planner.delimitedScope)) {
                planner.preValidators.add(validator);
            }

            return this;
        }

        public Builder preValidators(String scope, List<BiFunction<Policy, PolicyContext, Boolean>> validators) {
            validators.forEach(validator -> preValidator(scope, validator));
            return this;
        }

        public Builder postValidator(String scope, BiFunction<Policy, PolicyContext, Boolean> validator) {
            if (scopeFilter(scope, planner.delimitedScope)) {
                planner.postValidators.add(validator);
            }
            return this;
        }

        public Builder postValidators(String scope, List<BiFunction<Policy, PolicyContext, Boolean>> validators) {
            validators.forEach(validator -> postValidator(scope, validator));
            return this;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <R extends Rule> Builder evaluationFunction(String scope, String key, Class<R> ruleKind, AtomicConstraintFunction<R> function) {

            if (scopeFilter(scope, planner.delimitedScope)) {
                planner.constraintFunctions.computeIfAbsent(key, k -> new ArrayList<>())
                        .add(new ConstraintFunctionEntry(ruleKind, function));
            }
            return this;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <R extends Rule> Builder evaluationFunction(String scope, Class<R> ruleKind, DynamicAtomicConstraintFunction<R> function) {
            if (scopeFilter(scope, planner.delimitedScope)) {
                planner.dynamicConstraintFunctions.add(new DynamicAtomicConstraintFunctionEntry(ruleKind, function));
            }
            return this;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <R extends Rule> Builder evaluationFunction(String scope, Class<R> ruleKind, RuleFunction<R> function) {
            if (scopeFilter(scope, planner.delimitedScope)) {
                planner.ruleFunctions.add(new RuleFunctionFunctionEntry(ruleKind, function));
            }
            return this;
        }

        public PolicyEvaluationPlanner build() {
            Objects.requireNonNull(planner.ruleValidator, "Rule validator should not be null");
            return planner;
        }

        private boolean scopeFilter(String entry, String scope) {
            return ALL_SCOPES_DELIMITED.equals(entry) || scope.startsWith(entry);
        }
    }
}
