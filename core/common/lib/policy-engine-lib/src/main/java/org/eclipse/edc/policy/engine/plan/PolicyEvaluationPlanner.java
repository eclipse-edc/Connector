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

import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyValidatorRule;
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
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.policy.model.XoneConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.eclipse.edc.policy.engine.spi.PolicyEngine.DELIMITER;

public class PolicyEvaluationPlanner implements Policy.Visitor<PolicyEvaluationPlan>, Rule.Visitor<RuleStep<? extends Rule>>, Constraint.Visitor<ConstraintStep> {

    private final Stack<Rule> ruleContext = new Stack<>();
    private final List<PolicyValidatorRule<? extends PolicyContext>> preValidators = new ArrayList<>();
    private final List<PolicyValidatorRule<? extends PolicyContext>> postValidators = new ArrayList<>();
    private final Map<String, List<ConstraintFunctionEntry<Rule, ? extends PolicyContext>>> constraintFunctions = new TreeMap<>();
    private final List<DynamicAtomicConstraintFunctionEntry<Rule, ? extends PolicyContext>> dynamicConstraintFunctions = new ArrayList<>();
    private final List<RuleFunctionFunctionEntry<Rule, ? extends PolicyContext>> ruleFunctions = new ArrayList<>();
    private final String delimitedScope;
    private final String scope;

    private RuleValidator ruleValidator;

    private PolicyEvaluationPlanner(String scope) {
        this.scope = scope;
        this.delimitedScope = scope + DELIMITER;
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
        var functionName = getFunctionName(leftValue, currentRule.getClass());

        var filteringReasons = new ArrayList<String>();

        if (!ruleValidator.isInScope(leftValue, delimitedScope)) {
            filteringReasons.add("leftOperand '%s' is not bound to scope '%s'".formatted(leftValue, scope));
        }

        if (functionName == null) {
            filteringReasons.add("leftOperand '%s' is not bound to any function within scope '%s'".formatted(leftValue, scope));
        }

        return new AtomicConstraintStep(constraint, filteringReasons, currentRule, functionName);
    }

    @Override
    public PolicyEvaluationPlan visitPolicy(Policy policy) {

        var builder = PolicyEvaluationPlan.Builder.newInstance();

        preValidators.stream().map(ValidatorStep::new).forEach(builder::preValidator);
        postValidators.stream().map(ValidatorStep::new).forEach(builder::postValidator);

        policy.getPermissions().stream().map(permission -> permission.accept(this))
                .map(PermissionStep.class::cast)
                .forEach(builder::permission);

        policy.getObligations().stream().map(obligation -> obligation.accept(this))
                .map(DutyStep.class::cast)
                .forEach(builder::duty);

        policy.getProhibitions().stream().map(permission -> permission.accept(this))
                .map(ProhibitionStep.class::cast)
                .forEach(builder::prohibition);

        return builder.build();
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

    private String getFunctionName(String key, Class<? extends Rule> ruleKind) {
        return constraintFunctions.getOrDefault(key, new ArrayList<>())
                .stream()
                .filter(entry -> ruleKind.isAssignableFrom(entry.type()))
                .map(entry -> entry.function.name())
                .findFirst()
                .or(() -> dynamicConstraintFunctions
                        .stream()
                        .filter(f -> ruleKind.isAssignableFrom(f.type))
                        .filter(f -> f.function.canHandle(key))
                        .map(f -> f.function.name())
                        .findFirst())
                .orElse(null);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <R extends Rule> void visitRule(R rule, RuleStep.Builder builder) {

        try {
            ruleContext.push(rule);

            if (rule.getAction() != null && !ruleValidator.isBounded(rule.getAction().getType())) {
                builder.filtered(true);
                builder.filteringReason("action '%s' is not bound to scope '%s'".formatted(rule.getAction().getType(), scope));
            }
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

    private List<ConstraintStep> validateMultiplicityConstraint(MultiplicityConstraint multiplicityConstraint) {
        return multiplicityConstraint.getConstraints()
                .stream()
                .map(c -> c.accept(this))
                .collect(Collectors.toList());
    }

    private record ConstraintFunctionEntry<R extends Rule, C extends PolicyContext>(
            Class<R> type,
            AtomicConstraintRuleFunction<R, C> function) {
    }

    private record DynamicAtomicConstraintFunctionEntry<R extends Rule, C extends PolicyContext>(
            Class<R> type,
            DynamicAtomicConstraintRuleFunction<R, C> function) {
    }

    private record RuleFunctionFunctionEntry<R extends Rule, C extends PolicyContext>(
            Class<R> type,
            PolicyRuleFunction<R, C> function) {
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

        public <C extends PolicyContext> Builder preValidator(PolicyValidatorRule<C> validator) {
            planner.preValidators.add(validator);

            return this;
        }

        public <C extends PolicyContext> Builder postValidator(PolicyValidatorRule<C> validator) {
            planner.postValidators.add(validator);
            return this;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <R extends Rule, C extends PolicyContext> Builder evaluationFunction(String key, Class<R> ruleKind, AtomicConstraintRuleFunction<R, C> function) {
            planner.constraintFunctions.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new ConstraintFunctionEntry(ruleKind, function));
            return this;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <R extends Rule, C extends PolicyContext> Builder evaluationFunction(Class<R> ruleKind, DynamicAtomicConstraintRuleFunction<R, C> function) {
            planner.dynamicConstraintFunctions.add(new DynamicAtomicConstraintFunctionEntry(ruleKind, function));
            return this;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <R extends Rule, C extends PolicyContext> Builder evaluationFunction(Class<R> ruleKind, PolicyRuleFunction<R, C> function) {
            planner.ruleFunctions.add(new RuleFunctionFunctionEntry(ruleKind, function));
            return this;
        }

        public PolicyEvaluationPlanner build() {
            Objects.requireNonNull(planner.ruleValidator, "Rule validator should not be null");
            return planner;
        }

    }
}
