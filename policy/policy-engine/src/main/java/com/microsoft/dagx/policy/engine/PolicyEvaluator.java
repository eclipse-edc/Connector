package com.microsoft.dagx.policy.engine;

import com.microsoft.dagx.policy.model.AndConstraint;
import com.microsoft.dagx.policy.model.AtomicConstraint;
import com.microsoft.dagx.policy.model.AtomicConstraintFunction;
import com.microsoft.dagx.policy.model.Constraint;
import com.microsoft.dagx.policy.model.Duty;
import com.microsoft.dagx.policy.model.Expression;
import com.microsoft.dagx.policy.model.LiteralExpression;
import com.microsoft.dagx.policy.model.OrConstraint;
import com.microsoft.dagx.policy.model.Permission;
import com.microsoft.dagx.policy.model.Policy;
import com.microsoft.dagx.policy.model.Prohibition;
import com.microsoft.dagx.policy.model.Rule;
import com.microsoft.dagx.policy.model.XoneConstraint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Evaluates a policy.
 *
 * A policy evaluator is used to build evaluation engines that perform tasks such as verifying if a {@link Policy} is satisfied by a client system presenting signed credentials.
 * Implementation-specific functionality is contributed by registering {@link AtomicConstraintFunction}s using {@link Builder#permissionFunction(String, AtomicConstraintFunction)},
 * {@link Builder#prohibitionFunction(String, AtomicConstraintFunction)}, and  {@link Builder#dutyFunction(String, AtomicConstraintFunction)}.
 */
public class PolicyEvaluator implements Policy.Visitor<Boolean>, Rule.Visitor<Boolean>, Constraint.Visitor<Boolean>, Expression.Visitor<Object> {
    private Rule ruleContext; // the current rule being evaluated or null

    private List<RuleProblem> ruleProblems = new ArrayList<>();

    private Map<String, AtomicConstraintFunction<Object, Boolean, ? extends Rule>> permissionFunctions = new HashMap<>();
    private Map<String, AtomicConstraintFunction<Object, Boolean, ? extends Rule>> dutyFunctions = new HashMap<>();
    private Map<String, AtomicConstraintFunction<Object, Boolean, ? extends Rule>> prohibitionFunctions = new HashMap<>();

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
        try {
            ruleContext = permission;
            return visitRule(permission);
        } finally {
            ruleContext = null;
        }
    }

    @Override
    public Boolean visitProhibition(Prohibition prohibition) {
        try {
            ruleContext = prohibition;
            return visitRule(prohibition);
        } finally {
            ruleContext = null;
        }
    }

    @Override
    public Boolean visitDuty(Duty duty) {
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
        for (Constraint constraint : xoneConstraint.getConstraints()) {
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
        var rightValue = constraint.getRightExpression().accept(this);
        Object leftRawValue = constraint.getLeftExpression().accept(this);
        if (leftRawValue instanceof String) {
            AtomicConstraintFunction<Object, Boolean, Rule> function;
            if (ruleContext instanceof Permission) {
                //noinspection unchecked
                function = (AtomicConstraintFunction<Object, Boolean, Rule>) permissionFunctions.get(leftRawValue);
            } else if (ruleContext instanceof Prohibition) {
                //noinspection unchecked
                function = (AtomicConstraintFunction<Object, Boolean, Rule>) prohibitionFunctions.get(leftRawValue);
            } else {
                //noinspection unchecked
                function = (AtomicConstraintFunction<Object, Boolean, Rule>) dutyFunctions.get(leftRawValue);
            }
            if (function != null) {
                return function.evaluate(constraint.getOperator(), rightValue, ruleContext);
            }
        }

        // TODO handle expression eval errors
        switch (constraint.getOperator()) {
            case EQ:
                return Objects.equals(leftRawValue, rightValue);
            case NEQ:
                return !Objects.equals(leftRawValue, rightValue);
            case GT:
                break;
            case GEQ:
                break;
            case LT:
                break;
            case LEQ:
                break;
        }
        return null;
    }

    @Override
    public Object visitLiteralExpression(LiteralExpression expression) {
        return expression.getValue();
    }

    private Boolean visitRule(Rule rule) {
        var valid = true;
        RuleProblem.Builder problemBuilder = null;
        for (Constraint constraint : rule.getConstraints()) {
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

    private PolicyEvaluator() {
    }

    public static class Builder {
        private PolicyEvaluator evaluator;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder permissionFunction(String key, AtomicConstraintFunction<Object, Boolean, Permission> function) {
            evaluator.permissionFunctions.put(key, function);
            return this;
        }

        public Builder dutyFunction(String key, AtomicConstraintFunction<Object, Boolean, Duty> function) {
            evaluator.dutyFunctions.put(key, function);
            return this;
        }

        public Builder prohibitionFunction(String key, AtomicConstraintFunction<Object, Boolean, Prohibition> function) {
            evaluator.prohibitionFunctions.put(key, function);
            return this;
        }

        public PolicyEvaluator build() {
            return evaluator;
        }

        private Builder() {
            evaluator = new PolicyEvaluator();
        }
    }

}
