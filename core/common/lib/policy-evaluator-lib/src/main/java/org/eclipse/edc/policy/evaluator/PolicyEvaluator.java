/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.policy.evaluator;

import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.AtomicConstraintFunction;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.DynamicAtomicConstraintFunction;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.policy.model.RuleFunction;
import org.eclipse.edc.policy.model.XoneConstraint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Evaluates a policy.
 * <p>
 * A policy evaluator is used to build evaluation engines that perform tasks such as verifying if a {@link Policy} is satisfied by a client system presenting signed credentials.
 * Implementation-specific functionality is contributed by registering {@link AtomicConstraintFunction}s using {@link Builder#permissionFunction(String, AtomicConstraintFunction)},
 * {@link Builder#prohibitionFunction(String, AtomicConstraintFunction)}, and  {@link Builder#dutyFunction(String, AtomicConstraintFunction)}.
 */
public class PolicyEvaluator implements Policy.Visitor<Boolean>, Rule.Visitor<Boolean>, Constraint.Visitor<Boolean>, Expression.Visitor<Object> {
    private final List<RuleProblem> ruleProblems = new ArrayList<>();
    private final Map<String, AtomicConstraintFunction<Object, ? extends Rule, Boolean>> permissionFunctions = new HashMap<>();
    private final List<DynamicConstraintFunctionEntry<? extends Rule>> dynamicPermissionFunctions = new ArrayList<>();
    private final Map<String, AtomicConstraintFunction<Object, ? extends Rule, Boolean>> dutyFunctions = new HashMap<>();
    private final List<DynamicConstraintFunctionEntry<? extends Rule>> dynamicDutyFunctions = new ArrayList<>();
    private final Map<String, AtomicConstraintFunction<Object, ? extends Rule, Boolean>> prohibitionFunctions = new HashMap<>();
    private final List<DynamicConstraintFunctionEntry<? extends Rule>> dynamicProhibitionFunctions = new ArrayList<>();

    private final List<RuleFunction<Permission>> permissionRuleFunctions = new ArrayList<>();
    private final List<RuleFunction<Duty>> dutyRuleFunctions = new ArrayList<>();
    private final List<RuleFunction<Prohibition>> prohibitionRuleFunctions = new ArrayList<>();

    private Rule ruleContext; // the current rule being evaluated or null

    private PolicyEvaluator() {
    }

    public PolicyEvaluationResult evaluate(Policy policy) {
        return policy.accept(this) ? new PolicyEvaluationResult() : new PolicyEvaluationResult(ruleProblems);
    }

    @Override
    public Boolean visitPolicy(Policy policy) {
        policy.getPermissions().forEach(permission -> permission.accept(this));
        policy.getProhibitions().forEach(prohibition -> prohibition.accept(this));
        policy.getObligations().forEach(duty -> duty.accept(this));
        return ruleProblems.isEmpty();
    }

    @Override
    public Boolean visitPermission(Permission permission) {
        for (var function : permissionRuleFunctions) {
            if (!function.evaluate(permission)) {
                ruleProblems.add(RuleProblem.Builder.newInstance().rule(permission).description("Evalution failed for: " + permission.toString()).build());
                return false;
            }
        }
        try {
            if (permission.getDuties() != null) {
                for (var duty : permission.getDuties()) {
                    ruleContext = duty;
                    if (!visitRule(duty)) {
                        return false;
                    }
                }
            }
            ruleContext = permission;
            return visitRule(permission);
        } finally {
            ruleContext = null;
        }
    }

    @Override
    public Boolean visitProhibition(Prohibition prohibition) {
        for (var function : prohibitionRuleFunctions) {
            if (function.evaluate(prohibition)) {
                ruleProblems.add(RuleProblem.Builder.newInstance().rule(prohibition).description("Evalution failed for: " + prohibition.toString()).build());
                return false;
            }
        }
        try {
            ruleContext = prohibition;
            return visitRule(prohibition);
        } finally {
            ruleContext = null;
        }
    }

    @Override
    public Boolean visitDuty(Duty duty) {
        for (var function : dutyRuleFunctions) {
            if (!function.evaluate(duty)) {
                ruleProblems.add(RuleProblem.Builder.newInstance().rule(duty).description("Evalution failed for: " + duty.toString()).build());
                return false;
            }
        }
        try {
            ruleContext = duty;
            return visitRule(duty);
        } finally {
            ruleContext = null;
        }
    }

    @Override
    public Boolean visitAndConstraint(AndConstraint andConstraint) {
        return andConstraint.getConstraints().stream().allMatch(constraint -> constraint.accept(this));
    }

    @Override
    public Boolean visitOrConstraint(OrConstraint orConstraint) {
        return orConstraint.getConstraints().stream().anyMatch(constraint -> constraint.accept(this));
    }

    @Override
    public Boolean visitXoneConstraint(XoneConstraint xoneConstraint) {
        int count = 0;
        for (var constraint : xoneConstraint.getConstraints()) {
            if (constraint.accept(this)) {
                count++;
                if (count > 1) {
                    return false;
                }
            }
        }
        return count == 1;
    }

    @Override
    public Boolean visitAtomicConstraint(AtomicConstraint constraint) {
        var leftRawValue = constraint.getLeftExpression().accept(this);
        if (!(leftRawValue instanceof String)) {
            ruleProblems.add(RuleProblem.Builder.newInstance()
                    .rule(ruleContext)
                    .description(ruleContext.toString())
                    .constraintProblem(new ConstraintProblem("Left operand value is not a String", constraint))
                    .build());
            return false;
        }

        AtomicConstraintFunction<Object, Rule, Boolean> function;
        if (ruleContext instanceof Permission) {
            function = getEvaluationFunction((String) leftRawValue, permissionFunctions, dynamicPermissionFunctions);
        } else if (ruleContext instanceof Prohibition) {
            function = getEvaluationFunction((String) leftRawValue, prohibitionFunctions, dynamicProhibitionFunctions);
        } else {
            function = getEvaluationFunction((String) leftRawValue, dutyFunctions, dynamicDutyFunctions);
        }
        if (function == null) {
            ruleProblems.add(RuleProblem.Builder.newInstance()
                    .rule(ruleContext)
                    .description(ruleContext.toString())
                    .constraintProblem(new ConstraintProblem("No evaluation function found", constraint))
                    .build());
            return false;
        }

        var rightValue = constraint.getRightExpression().accept(this);
        return function.evaluate(constraint.getOperator(), rightValue, ruleContext);
    }

    @Override
    public Object visitLiteralExpression(LiteralExpression expression) {
        return expression.getValue();
    }

    @SuppressWarnings("unchecked")
    private AtomicConstraintFunction<Object, Rule, Boolean> getEvaluationFunction(String leftRawValue,
                                                                                  Map<String, AtomicConstraintFunction<Object, ? extends Rule, Boolean>> functions,
                                                                                  List<DynamicConstraintFunctionEntry<? extends Rule>> dynamicFunctions) {

        return Optional.ofNullable((AtomicConstraintFunction<Object, Rule, Boolean>) functions.get(leftRawValue))
                .orElseGet(() -> getDynamicFunction(leftRawValue, dynamicFunctions));
    }

    @SuppressWarnings("unchecked")
    private AtomicConstraintFunction<Object, Rule, Boolean> getDynamicFunction(String leftRawValue, List<DynamicConstraintFunctionEntry<? extends Rule>> dynamicFunctions) {
        return dynamicFunctions.stream()
                .filter(entry -> entry.guard.apply(leftRawValue))
                .map(entry -> (DynamicAtomicConstraintFunction<Object, Object, Rule, Boolean>) entry.function)
                .map(dynamicFunction -> wrapDynamicFunction(leftRawValue, dynamicFunction))
                .findFirst()
                .orElse(null);

    }

    private AtomicConstraintFunction<Object, Rule, Boolean> wrapDynamicFunction(String leftRawValue, DynamicAtomicConstraintFunction<Object, Object, Rule, Boolean> dynamicFunction) {
        return (operator, rightValue, rule) -> dynamicFunction.evaluate(leftRawValue, operator, rightValue, rule);
    }

    private Boolean visitRule(Rule rule) {
        var valid = true;
        RuleProblem.Builder problemBuilder = null;
        for (var constraint : rule.getConstraints()) {
            var result = constraint.accept(this);
            if ((!result && !(ruleContext instanceof Prohibition) || (result && ruleContext instanceof Prohibition))) {
                if (problemBuilder == null) {
                    problemBuilder = RuleProblem.Builder.newInstance().rule(rule).description(rule.toString());
                }
                var message = ruleContext instanceof Prohibition ? "Prohibited constraint evaluated true" : "Constraint evaluated false";
                problemBuilder.constraintProblem(new ConstraintProblem(message + " => " + constraint, constraint));
                valid = false;
            }
        }
        if (!valid) {
            ruleProblems.add(problemBuilder.build());
        }
        return valid;
    }

    record DynamicConstraintFunctionEntry<R extends Rule>(Function<Object, Boolean> guard,
                                                          DynamicAtomicConstraintFunction<Object, Object, R, Boolean> function) {
    }


    public static class Builder {
        private final PolicyEvaluator evaluator;

        private Builder() {
            evaluator = new PolicyEvaluator();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder permissionFunction(String key, AtomicConstraintFunction<Object, Permission, Boolean> function) {
            evaluator.permissionFunctions.put(key, function);
            return this;
        }

        public Builder dynamicPermissionFunction(Function<Object, Boolean> guard, DynamicAtomicConstraintFunction<Object, Object, Permission, Boolean> function) {
            evaluator.dynamicPermissionFunctions.add(new DynamicConstraintFunctionEntry<>(guard, function));
            return this;
        }

        public Builder dutyFunction(String key, AtomicConstraintFunction<Object, Duty, Boolean> function) {
            evaluator.dutyFunctions.put(key, function);
            return this;
        }

        public Builder dynamicDutyFunction(Function<Object, Boolean> guard, DynamicAtomicConstraintFunction<Object, Object, Duty, Boolean> function) {
            evaluator.dynamicDutyFunctions.add(new DynamicConstraintFunctionEntry<>(guard, function));
            return this;
        }

        public Builder prohibitionFunction(String key, AtomicConstraintFunction<Object, Prohibition, Boolean> function) {
            evaluator.prohibitionFunctions.put(key, function);
            return this;
        }

        public Builder dynamicProhibitionFunction(Function<Object, Boolean> guard, DynamicAtomicConstraintFunction<Object, Object, Prohibition, Boolean> function) {
            evaluator.dynamicProhibitionFunctions.add(new DynamicConstraintFunctionEntry<>(guard, function));
            return this;
        }

        public Builder permissionRuleFunction(RuleFunction<Permission> function) {
            evaluator.permissionRuleFunctions.add(function);
            return this;
        }

        public Builder dutyRuleFunction(RuleFunction<Duty> function) {
            evaluator.dutyRuleFunctions.add(function);
            return this;
        }

        public Builder prohibitionRuleFunction(RuleFunction<Prohibition> function) {
            evaluator.prohibitionRuleFunctions.add(function);
            return this;
        }

        public PolicyEvaluator build() {
            return evaluator;
        }
    }

}
