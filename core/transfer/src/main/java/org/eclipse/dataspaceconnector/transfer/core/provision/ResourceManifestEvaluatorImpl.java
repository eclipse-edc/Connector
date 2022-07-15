/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.provision;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.policy.model.Rule;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.evaluation.ResourceDefinitionAtomicConstraintFunction;
import org.eclipse.dataspaceconnector.spi.transfer.provision.evaluation.ResourceDefinitionRuleFunction;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.provision.evaluation.ResourceManifestEvaluator;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;

import static java.lang.String.format;

public class ResourceManifestEvaluatorImpl implements ResourceManifestEvaluator {
    
    private final Map<Class<? extends ResourceDefinition>, List<ResourceDefinitionRuleFunction<Permission, ? extends ResourceDefinition>>> resourceDefinitionPermissionFunctions = new HashMap<>();
    private final Map<Class<? extends ResourceDefinition>, List<ResourceDefinitionRuleFunction<Prohibition, ? extends ResourceDefinition>>> resourceDefinitionProhibitionFunctions = new HashMap<>();
    private final Map<Class<? extends ResourceDefinition>, List<ResourceDefinitionRuleFunction<Duty, ? extends ResourceDefinition>>> resourceDefinitionDutyFunctions = new HashMap<>();
    
    private final Map<ResourceDefinitionConstraintFunctionKey, ResourceDefinitionAtomicConstraintFunction<Permission, ? extends ResourceDefinition>> resourceDefinitionConstraintPermissionFunctions = new HashMap<>();
    private final Map<ResourceDefinitionConstraintFunctionKey, ResourceDefinitionAtomicConstraintFunction<Prohibition, ? extends ResourceDefinition>> resourceDefinitionConstraintProhibitionFunctions = new HashMap<>();
    private final Map<ResourceDefinitionConstraintFunctionKey, ResourceDefinitionAtomicConstraintFunction<Duty, ? extends ResourceDefinition>> resourceDefinitionConstraintDutyFunctions = new HashMap<>();
    
    private final Monitor monitor;
    
    public ResourceManifestEvaluatorImpl(Monitor monitor) {
        this.monitor = monitor;
    }
    
    @Override
    public boolean evaluate(Policy policy, PolicyContext policyContext) {
        var manifest = policyContext.getContextData(ResourceManifest.class);
        if (manifest == null) {
            // not evaluating a manifest
            return true;
        }
        
        var result = evaluateManifest(manifest, policy);
        if (result.failed()) {
            result.getFailureMessages().forEach(policyContext::reportProblem);
            return false;
        } else {
            policyContext.putContextData(ResourceManifest.class, result.getContent());
            return true;
        }
    }
    
    private Result<ResourceManifest> evaluateManifest(ResourceManifest resourceManifest, Policy policy) {
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
        
        for (var permission: policy.getPermissions()) {
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
                monitor.warning(format("Constraint of type %s not supported for resource manifest evaluation. Ignoring.", constraint.getClass().getSimpleName()));
                continue;
            }
            
            var atomicConstraint = (AtomicConstraint) constraint;
            Object leftRawValue = ((LiteralExpression) atomicConstraint.getLeftExpression()).getValue();
            if (!(leftRawValue instanceof String)) {
                monitor.warning(format("Left expression of type %s not supported for resource manifest evaluation. Ignoring.", leftRawValue.getClass().getSimpleName()));
                continue;
            }
            
            var functionKey = new ResourceDefinitionConstraintFunctionKey((String) leftRawValue, resourceDefinition.getClass());
            var function = functions.get(functionKey);
            if (function == null) {
                // No function available for evaluating the given constraint
                monitor.warning(format("No function available for evaluating constraint '%s' for resource definition of type %s. Ignoring.", leftRawValue, resourceDefinition.getClass()));
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
    
    @Override
    @SuppressWarnings({"unchecked"})
    public <D extends ResourceDefinition, R extends Rule> void registerFunction(Class<D> resourceType, Class<R> ruleType, ResourceDefinitionRuleFunction<R, D> function) {
        if (Permission.class.isAssignableFrom(ruleType)) {
            resourceDefinitionPermissionFunctions.computeIfAbsent(resourceType, k -> new ArrayList<>())
                    .add((ResourceDefinitionRuleFunction<Permission, ? extends ResourceDefinition>) function);
        } else if (Prohibition.class.isAssignableFrom(ruleType)) {
            resourceDefinitionProhibitionFunctions.computeIfAbsent(resourceType, k -> new ArrayList<>())
                    .add((ResourceDefinitionRuleFunction<Prohibition, ? extends ResourceDefinition>) function);
        } else if (Duty.class.isAssignableFrom(ruleType)) {
            resourceDefinitionDutyFunctions.computeIfAbsent(resourceType, k -> new ArrayList<>())
                    .add((ResourceDefinitionRuleFunction<Duty, ? extends ResourceDefinition>) function);
        }
    }
    
    @Override
    @SuppressWarnings({"unchecked"})
    public <D extends ResourceDefinition, R extends Rule> void registerFunction(String key,
                                                                                Class<D> resourceType, Class<R> ruleType, ResourceDefinitionAtomicConstraintFunction<R, D> function) {
        if (Permission.class.isAssignableFrom(ruleType)) {
            resourceDefinitionConstraintPermissionFunctions.put(new ResourceDefinitionConstraintFunctionKey(key, resourceType),
                    (ResourceDefinitionAtomicConstraintFunction<Permission, ? extends ResourceDefinition>) function);
        } else if (Prohibition.class.isAssignableFrom(ruleType)) {
            resourceDefinitionConstraintProhibitionFunctions.put(new ResourceDefinitionConstraintFunctionKey(key, resourceType),
                    (ResourceDefinitionAtomicConstraintFunction<Prohibition, ? extends ResourceDefinition>) function);
        } else if (Duty.class.isAssignableFrom(ruleType)) {
            resourceDefinitionConstraintDutyFunctions.put(new ResourceDefinitionConstraintFunctionKey(key, resourceType),
                    (ResourceDefinitionAtomicConstraintFunction<Duty, ? extends ResourceDefinition>) function);
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
