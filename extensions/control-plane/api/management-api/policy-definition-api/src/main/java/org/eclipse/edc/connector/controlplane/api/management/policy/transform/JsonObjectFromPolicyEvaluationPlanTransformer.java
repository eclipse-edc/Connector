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

package org.eclipse.edc.connector.controlplane.api.management.policy.transform;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan;
import org.eclipse.edc.policy.engine.spi.plan.step.AndConstraintStep;
import org.eclipse.edc.policy.engine.spi.plan.step.AtomicConstraintStep;
import org.eclipse.edc.policy.engine.spi.plan.step.ConstraintStep;
import org.eclipse.edc.policy.engine.spi.plan.step.DutyStep;
import org.eclipse.edc.policy.engine.spi.plan.step.MultiplicityConstraintStep;
import org.eclipse.edc.policy.engine.spi.plan.step.OrConstraintStep;
import org.eclipse.edc.policy.engine.spi.plan.step.PermissionStep;
import org.eclipse.edc.policy.engine.spi.plan.step.ProhibitionStep;
import org.eclipse.edc.policy.engine.spi.plan.step.RuleFunctionStep;
import org.eclipse.edc.policy.engine.spi.plan.step.RuleStep;
import org.eclipse.edc.policy.engine.spi.plan.step.ValidatorStep;
import org.eclipse.edc.policy.engine.spi.plan.step.XoneConstraintStep;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_OBLIGATION_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_PERMISSION_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_POST_VALIDATORS;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_PRE_VALIDATORS;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_PROHIBITION_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_TYPE;
import static org.eclipse.edc.policy.engine.spi.plan.step.AndConstraintStep.EDC_AND_CONSTRAINT_STEP_TYPE;
import static org.eclipse.edc.policy.engine.spi.plan.step.AtomicConstraintStep.EDC_ATOMIC_CONSTRAINT_STEP_FILTERING_REASONS;
import static org.eclipse.edc.policy.engine.spi.plan.step.AtomicConstraintStep.EDC_ATOMIC_CONSTRAINT_STEP_FUNCTION_NAME;
import static org.eclipse.edc.policy.engine.spi.plan.step.AtomicConstraintStep.EDC_ATOMIC_CONSTRAINT_STEP_FUNCTION_PARAMS;
import static org.eclipse.edc.policy.engine.spi.plan.step.AtomicConstraintStep.EDC_ATOMIC_CONSTRAINT_STEP_IS_FILTERED;
import static org.eclipse.edc.policy.engine.spi.plan.step.AtomicConstraintStep.EDC_ATOMIC_CONSTRAINT_STEP_TYPE;
import static org.eclipse.edc.policy.engine.spi.plan.step.DutyStep.EDC_DUTY_STEP_TYPE;
import static org.eclipse.edc.policy.engine.spi.plan.step.MultiplicityConstraintStep.EDC_MULTIPLICITY_CONSTRAINT_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.step.OrConstraintStep.EDC_OR_CONSTRAINT_STEP_TYPE;
import static org.eclipse.edc.policy.engine.spi.plan.step.PermissionStep.EDC_PERMISSION_STEP_DUTY_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.step.PermissionStep.EDC_PERMISSION_STEP_TYPE;
import static org.eclipse.edc.policy.engine.spi.plan.step.ProhibitionStep.EDC_PROHIBITION_STEP_TYPE;
import static org.eclipse.edc.policy.engine.spi.plan.step.RuleStep.EDC_RULE_CONSTRAINT_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.step.RuleStep.EDC_RULE_FUNCTIONS;
import static org.eclipse.edc.policy.engine.spi.plan.step.RuleStep.EDC_RULE_STEP_FILTERING_REASONS;
import static org.eclipse.edc.policy.engine.spi.plan.step.RuleStep.EDC_RULE_STEP_IS_FILTERED;
import static org.eclipse.edc.policy.engine.spi.plan.step.XoneConstraintStep.EDC_XONE_CONSTRAINT_STEP_TYPE;

public class JsonObjectFromPolicyEvaluationPlanTransformer extends AbstractJsonLdTransformer<PolicyEvaluationPlan, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromPolicyEvaluationPlanTransformer(JsonBuilderFactory jsonFactory) {
        super(PolicyEvaluationPlan.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull PolicyEvaluationPlan plan, @NotNull TransformerContext context) {
        var objectBuilder = jsonFactory.createObjectBuilder();
        objectBuilder.add(TYPE, EDC_POLICY_EVALUATION_PLAN_TYPE);

        var preValidators = jsonFactory.createArrayBuilder();
        plan.getPreValidators().stream().map(ValidatorStep::name).forEach(preValidators::add);

        var postValidators = jsonFactory.createArrayBuilder();
        plan.getPostValidators().stream().map(ValidatorStep::name).forEach(postValidators::add);

        var permissionSteps = jsonFactory.createArrayBuilder();
        plan.getPermissionSteps().stream().map(this::transformPermissionStep)
                .forEach(permissionSteps::add);

        var prohibitionSteps = jsonFactory.createArrayBuilder();
        plan.getProhibitionSteps().stream().map(this::transformProhibitionStep)
                .forEach(prohibitionSteps::add);

        var dutySteps = jsonFactory.createArrayBuilder();
        plan.getObligationSteps().stream().map(this::transformDutyStep)
                .forEach(dutySteps::add);

        objectBuilder.add(EDC_POLICY_EVALUATION_PLAN_PRE_VALIDATORS, preValidators);
        objectBuilder.add(EDC_POLICY_EVALUATION_PLAN_PERMISSION_STEPS, permissionSteps);
        objectBuilder.add(EDC_POLICY_EVALUATION_PLAN_PROHIBITION_STEPS, prohibitionSteps);
        objectBuilder.add(EDC_POLICY_EVALUATION_PLAN_OBLIGATION_STEPS, dutySteps);
        objectBuilder.add(EDC_POLICY_EVALUATION_PLAN_POST_VALIDATORS, postValidators);

        return objectBuilder.build();
    }

    private JsonObjectBuilder transformDutyStep(DutyStep dutyStep) {
        return transformRuleStep(dutyStep, EDC_DUTY_STEP_TYPE);
    }

    private JsonObjectBuilder transformPermissionStep(PermissionStep permissionStep) {
        var dutySteps = jsonFactory.createArrayBuilder();

        permissionStep.getDutySteps().stream().map(this::transformDutyStep)
                .forEach(dutySteps::add);

        return transformRuleStep(permissionStep, EDC_PERMISSION_STEP_TYPE)
                .add(EDC_PERMISSION_STEP_DUTY_STEPS, dutySteps);
    }

    private JsonObjectBuilder transformProhibitionStep(ProhibitionStep prohibitionStep) {
        return transformRuleStep(prohibitionStep, EDC_PROHIBITION_STEP_TYPE);
    }

    private <R extends Rule> JsonObjectBuilder transformRuleStep(RuleStep<R> ruleStep, String type) {

        var builder = jsonFactory.createObjectBuilder();
        var constraintSteps = jsonFactory.createArrayBuilder();
        var ruleFunctionSteps = jsonFactory.createArrayBuilder();

        ruleStep.getConstraintSteps().stream()
                .map(this::transformConstraintStep)
                .forEach(constraintSteps::add);

        ruleStep.getRuleFunctions().stream()
                .map(RuleFunctionStep::functionName)
                .forEach(ruleFunctionSteps::add);

        builder.add(TYPE, type);
        builder.add(EDC_RULE_STEP_IS_FILTERED, ruleStep.isFiltered());
        builder.add(EDC_RULE_STEP_FILTERING_REASONS, jsonFactory.createArrayBuilder(ruleStep.getFilteringReasons()));
        builder.add(EDC_RULE_FUNCTIONS, ruleFunctionSteps);
        builder.add(EDC_RULE_CONSTRAINT_STEPS, constraintSteps);

        return builder;
    }

    private JsonObject transformConstraintStep(ConstraintStep constraintStep) {
        // TODO replace with pattern matching once we move to JDK 21
        if (constraintStep instanceof AtomicConstraintStep atomicConstraintStep) {
            return transformAtomicConstraintStep(atomicConstraintStep);
        } else if (constraintStep instanceof AndConstraintStep andConstraintStep) {
            return transformAndConstraintStep(andConstraintStep);
        } else if (constraintStep instanceof OrConstraintStep orConstraintStep) {
            return transformOrConstraintStep(orConstraintStep);
        } else if (constraintStep instanceof XoneConstraintStep xoneConstraintStep) {
            return transformXoneConstraintStep(xoneConstraintStep);
        }
        return jsonFactory.createObjectBuilder().build();
    }

    private JsonObject transformAtomicConstraintStep(AtomicConstraintStep atomicConstraintStep) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(TYPE, EDC_ATOMIC_CONSTRAINT_STEP_TYPE);
        builder.add(EDC_ATOMIC_CONSTRAINT_STEP_IS_FILTERED, atomicConstraintStep.isFiltered());
        builder.add(EDC_ATOMIC_CONSTRAINT_STEP_FILTERING_REASONS, jsonFactory.createArrayBuilder(atomicConstraintStep.filteringReasons()));

        Optional.ofNullable(atomicConstraintStep.functionName())
                .ifPresent(name -> builder.add(EDC_ATOMIC_CONSTRAINT_STEP_FUNCTION_NAME, name));

        builder.add(EDC_ATOMIC_CONSTRAINT_STEP_FUNCTION_PARAMS, jsonFactory.createArrayBuilder(atomicConstraintStep.functionParams()));
        return builder.build();
    }

    private JsonObject transformOrConstraintStep(OrConstraintStep orConstraintStep) {
        return transformMultiplicityConstraintStep(orConstraintStep, EDC_OR_CONSTRAINT_STEP_TYPE);
    }

    private JsonObject transformAndConstraintStep(AndConstraintStep andConstraintStep) {
        return transformMultiplicityConstraintStep(andConstraintStep, EDC_AND_CONSTRAINT_STEP_TYPE);

    }

    private JsonObject transformXoneConstraintStep(XoneConstraintStep xoneConstraintStep) {
        return transformMultiplicityConstraintStep(xoneConstraintStep, EDC_XONE_CONSTRAINT_STEP_TYPE);
    }

    private JsonObject transformMultiplicityConstraintStep(MultiplicityConstraintStep<? extends MultiplicityConstraint> multiplicityConstraintStep, String type) {
        var builder = jsonFactory.createObjectBuilder();
        var constraintSteps = jsonFactory.createArrayBuilder();

        multiplicityConstraintStep.getConstraintSteps().stream().map(this::transformConstraintStep)
                .forEach(constraintSteps::add);

        builder.add(TYPE, type);
        builder.add(EDC_MULTIPLICITY_CONSTRAINT_STEPS, constraintSteps);
        return builder.build();
    }
}
