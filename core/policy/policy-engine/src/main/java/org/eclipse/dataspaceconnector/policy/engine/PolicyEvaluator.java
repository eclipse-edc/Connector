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
 *       Fraunhofer Institute for Software and Systems Engineering - resource manifest evaluation
 *
 */

package org.eclipse.dataspaceconnector.policy.engine;

import org.eclipse.dataspaceconnector.policy.model.AndConstraint;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Expression;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.OrConstraint;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.policy.model.Rule;
import org.eclipse.dataspaceconnector.policy.model.XoneConstraint;
import org.eclipse.dataspaceconnector.spi.policy.evaluation.ResourceDefinitionAtomicConstraintFunction;
import org.eclipse.dataspaceconnector.spi.policy.evaluation.ResourceDefinitionRuleFunction;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;

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
    private final List<RuleProblem> ruleProblems = new ArrayList<>();

    private final Map<String, AtomicConstraintFunction<Object, ? extends Rule, Boolean>> permissionFunctions = new HashMap<>();
    private final Map<String, AtomicConstraintFunction<Object, ? extends Rule, Boolean>> dutyFunctions = new HashMap<>();
    private final Map<String, AtomicConstraintFunction<Object, ? extends Rule, Boolean>> prohibitionFunctions = new HashMap<>();

    private final List<RuleFunction<Permission>> permissionRuleFunctions = new ArrayList<>();
    private final List<RuleFunction<Duty>> dutyRuleFunctions = new ArrayList<>();
    private final List<RuleFunction<Prohibition>> prohibitionRuleFunctions = new ArrayList<>();

    private final Map<Class<? extends ResourceDefinition>, List<ResourceDefinitionRuleFunction<Permission, ? extends ResourceDefinition>>> resourceDefinitionPermissionFunctions = new HashMap<>();
    private final Map<Class<? extends ResourceDefinition>, List<ResourceDefinitionRuleFunction<Prohibition, ? extends ResourceDefinition>>> resourceDefinitionProhibitionFunctions = new HashMap<>();
    private final Map<Class<? extends ResourceDefinition>, List<ResourceDefinitionRuleFunction<Duty, ? extends ResourceDefinition>>> resourceDefinitionDutyFunctions = new HashMap<>();
    
    private final Map<ResourceDefinitionConstraintFunctionKey, ResourceDefinitionAtomicConstraintFunction<Permission, ? extends ResourceDefinition>> resourceDefinitionConstraintPermissionFunctions = new HashMap<>();
    private final Map<ResourceDefinitionConstraintFunctionKey, ResourceDefinitionAtomicConstraintFunction<Prohibition, ? extends ResourceDefinition>> resourceDefinitionConstraintProhibitionFunctions = new HashMap<>();
    private final Map<ResourceDefinitionConstraintFunctionKey, ResourceDefinitionAtomicConstraintFunction<Duty, ? extends ResourceDefinition>> resourceDefinitionConstraintDutyFunctions = new HashMap<>();

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
        for (RuleFunction<Permission> function : permissionRuleFunctions) {
            if (!function.evaluate(permission)) {
                ruleProblems.add(RuleProblem.Builder.newInstance().rule(permission).description("Evaluation failed for: " + permission.toString()).build());
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
        for (RuleFunction<Prohibition> function : prohibitionRuleFunctions) {
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
        for (RuleFunction<Duty> function : dutyRuleFunctions) {
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
            AtomicConstraintFunction<Object, Rule, Boolean> function;
            if (ruleContext instanceof Permission) {
                function = (AtomicConstraintFunction<Object, Rule, Boolean>) permissionFunctions.get(leftRawValue);
            } else if (ruleContext instanceof Prohibition) {
                function = (AtomicConstraintFunction<Object, Rule, Boolean>) prohibitionFunctions.get(leftRawValue);
            } else {
                function = (AtomicConstraintFunction<Object, Rule, Boolean>) dutyFunctions.get(leftRawValue);
            }
            if (function != null) {
                return function.evaluate(constraint.getOperator(), rightValue, ruleContext);
            }
        }

        // TODO handle expression eval errors
        switch (constraint.getOperator()) {
            case EQ:
                return Objects.equals(leftRawValue, rightValue);
            case IN:
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
            default:
                break;
        }
        return null;
    }

    @Override
    public Object visitLiteralExpression(LiteralExpression expression) {
        return expression.getValue();
    }
    
    /**
     * Evaluates and, if required, modifies the given ResourceManifest so that the given policy
     * is fulfilled. Internally evaluates each {@link ResourceDefinition} that's part of the
     * manifest separately.
     *
     * @param resourceManifest the resource manifest to evaluate.
     * @param policy the policy.
     * @return a result containing either the verified/modified resource manifest or the problems
     *         encountered during evaluation.
     */
    public Result<ResourceManifest> evaluateManifest(ResourceManifest resourceManifest, Policy policy) {
        var modifiedResourceDefinitions = new ArrayList<ResourceDefinition>();
        var failures = new ArrayList<String>();
        
        for (var resourceDefinition : resourceManifest.getDefinitions()) {
            var result = evaluateResourceDefinition(resourceDefinition, policy);
            if (result.succeeded()) {
                modifiedResourceDefinitions.add(result.getContent());
            } else {
                failures.addAll(result.getFailureMessages());
            }
        }
        
        return failures.isEmpty() ? Result.success(ResourceManifest.Builder.newInstance().definitions(modifiedResourceDefinitions).build()) : Result.failure(failures);
    }

    private <D extends ResourceDefinition> Result<D> evaluateResourceDefinition(D resourceDefinition, Policy policy) {
        var failures = new ArrayList<String>();
        
        for (var permission : policy.getPermissions()) {
            var result = visitResourceDefinitionRule(permission, resourceDefinition, resourceDefinitionPermissionFunctions);
            resourceDefinition = processResourceDefinitionResult(result, resourceDefinition, failures);
            
            result = visitResourceDefinitionConstraints(permission, resourceDefinition, resourceDefinitionConstraintPermissionFunctions);
            resourceDefinition = processResourceDefinitionResult(result, resourceDefinition, failures);
        }
        
        for (var prohibition : policy.getProhibitions()) {
            var result = visitResourceDefinitionRule(prohibition, resourceDefinition, resourceDefinitionProhibitionFunctions);
            resourceDefinition = processResourceDefinitionResult(result, resourceDefinition, failures);

            result = visitResourceDefinitionConstraints(prohibition, resourceDefinition, resourceDefinitionConstraintProhibitionFunctions);
            resourceDefinition = processResourceDefinitionResult(result, resourceDefinition, failures);
        }

        for (var duty : policy.getObligations()) {
            var result = visitResourceDefinitionRule(duty, resourceDefinition, resourceDefinitionDutyFunctions);
            resourceDefinition = processResourceDefinitionResult(result, resourceDefinition, failures);
            
            result = visitResourceDefinitionConstraints(duty, resourceDefinition, resourceDefinitionConstraintDutyFunctions);
            resourceDefinition = processResourceDefinitionResult(result, resourceDefinition, failures);
        }
    
        return failures.isEmpty() ? Result.success(resourceDefinition) : Result.failure(failures);
    }

    @SuppressWarnings({"unchecked"})
    private <D extends ResourceDefinition, R extends Rule> Result<D> visitResourceDefinitionConstraints(
            R rule, D resourceDefinition,
            Map<ResourceDefinitionConstraintFunctionKey, ResourceDefinitionAtomicConstraintFunction<R, ? extends ResourceDefinition>> functions) {
        var failures = new ArrayList<String>();
        
        for (var constraint : rule.getConstraints()) {
            if (!(constraint instanceof AtomicConstraint)) {
                continue;
            }
        
            var atomicConstraint = (AtomicConstraint) constraint;
            Object leftRawValue = atomicConstraint.getLeftExpression().accept(this);
            if (!(leftRawValue instanceof String)) {
                continue;
            }
        
            var functionKey = new ResourceDefinitionConstraintFunctionKey((String) leftRawValue, resourceDefinition.getClass());
            var function = functions.get(functionKey);
            if (function == null) {
                // No function available for evaluating the given constraint
                continue;
            }
            
            var result = ((ResourceDefinitionAtomicConstraintFunction<R, D>) function).evaluate(atomicConstraint.getOperator(), atomicConstraint.getRightExpression(), rule, resourceDefinition);
            resourceDefinition = processResourceDefinitionResult(result, resourceDefinition, failures);
        }
        
        return failures.isEmpty() ? Result.success(resourceDefinition) : Result.failure(failures);
    }
    
    @SuppressWarnings({"unchecked"})
    private <D extends ResourceDefinition, R extends Rule> Result<D> visitResourceDefinitionRule(
            R rule, D resourceDefinition, Map<Class<? extends ResourceDefinition>, List<ResourceDefinitionRuleFunction<R, ? extends ResourceDefinition>>> functions) {
        var failures = new ArrayList<String>();
    
        var functionsForType = functions.get(resourceDefinition.getClass());
        if (functionsForType != null) {
            for (var function : functionsForType) {
                var result = ((ResourceDefinitionRuleFunction<R, D>) function).evaluate(rule, resourceDefinition);
                resourceDefinition = processResourceDefinitionResult(result, resourceDefinition, failures);
            }
        }
    
        return failures.isEmpty() ? Result.success(resourceDefinition) : Result.failure(failures);
    }
    
    private <D extends ResourceDefinition> D processResourceDefinitionResult(Result<D> result, D resourceDefinition, List<String> failures) {
        if (result.succeeded()) {
            return result.getContent();
        } else {
            failures.addAll(result.getFailureMessages());
            return resourceDefinition;
        }
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

        public Builder dutyFunction(String key, AtomicConstraintFunction<Object, Duty, Boolean> function) {
            evaluator.dutyFunctions.put(key, function);
            return this;
        }

        public Builder prohibitionFunction(String key, AtomicConstraintFunction<Object, Prohibition, Boolean> function) {
            evaluator.prohibitionFunctions.put(key, function);
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
        
        public <D extends ResourceDefinition> Builder resourceDefinitionPermissionFunction(Class<D> resourceType, ResourceDefinitionRuleFunction<Permission, D> function) {
            evaluator.resourceDefinitionPermissionFunctions.computeIfAbsent(resourceType, k -> new ArrayList<>()).add(function);
            return this;
        }
    
        public <D extends ResourceDefinition> Builder resourceDefinitionProhibitionFunction(Class<D> resourceType, ResourceDefinitionRuleFunction<Prohibition, D> function) {
            evaluator.resourceDefinitionProhibitionFunctions.computeIfAbsent(resourceType, k -> new ArrayList<>()).add(function);
            return this;
        }
    
        public <D extends ResourceDefinition> Builder resourceDefinitionDutyFunction(Class<D> resourceType, ResourceDefinitionRuleFunction<Duty, D> function) {
            evaluator.resourceDefinitionDutyFunctions.computeIfAbsent(resourceType, k -> new ArrayList<>()).add(function);
            return this;
        }
    
        public <D extends ResourceDefinition> Builder resourceDefinitionConstraintPermissionFunction(String key, Class<D> resourceType, ResourceDefinitionAtomicConstraintFunction<Permission, D> function) {
            evaluator.resourceDefinitionConstraintPermissionFunctions.put(new ResourceDefinitionConstraintFunctionKey(key, resourceType), function);
            return this;
        }
    
        public <D extends ResourceDefinition> Builder resourceDefinitionConstraintProhibitionFunction(String key, Class<D> resourceType, ResourceDefinitionAtomicConstraintFunction<Prohibition, D> function) {
            evaluator.resourceDefinitionConstraintProhibitionFunctions.put(new ResourceDefinitionConstraintFunctionKey(key, resourceType), function);
            return this;
        }
    
        public <D extends ResourceDefinition> Builder resourceDefinitionConstraintDutyFunction(String key, Class<D> resourceType, ResourceDefinitionAtomicConstraintFunction<Duty, D> function) {
            evaluator.resourceDefinitionConstraintDutyFunctions.put(new ResourceDefinitionConstraintFunctionKey(key, resourceType), function);
            return this;
        }

        public PolicyEvaluator build() {
            return evaluator;
        }
    }
    
    static class ResourceDefinitionConstraintFunctionKey {
        private final String key;
        private final Class<? extends ResourceDefinition> definitionType;
        
        ResourceDefinitionConstraintFunctionKey(String key, Class<? extends ResourceDefinition> definitionType) {
            this.key = Objects.requireNonNull(key);
            this.definitionType = Objects.requireNonNull(definitionType);
        }
        
        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            
            if (!(object instanceof ResourceDefinitionConstraintFunctionKey)) {
                return false;
            }
            
            var other = (ResourceDefinitionConstraintFunctionKey) object;
            return other.key.equals(this.key) && other.definitionType.equals(definitionType);
        }
    
        @Override
        public int hashCode() {
            var result = 1;
            result = 31 * result + key.hashCode();
            result = 31 * result + definitionType.hashCode();
            return result;
        }
        
    }

}
