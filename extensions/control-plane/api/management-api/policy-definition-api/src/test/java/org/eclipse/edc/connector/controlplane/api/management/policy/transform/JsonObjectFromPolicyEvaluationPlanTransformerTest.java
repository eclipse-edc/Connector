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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
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
import org.eclipse.edc.policy.engine.spi.plan.step.ValidatorStep;
import org.eclipse.edc.policy.engine.spi.plan.step.XoneConstraintStep;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_OBLIGATION_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_PERMISSION_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_POST_VALIDATORS;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_PRE_VALIDATORS;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_PROHIBITION_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.step.AndConstraintStep.EDC_AND_CONSTRAINT_STEP_TYPE;
import static org.eclipse.edc.policy.engine.spi.plan.step.AtomicConstraintStep.EDC_ATOMIC_CONSTRAINT_STEP_FILTERING_REASONS;
import static org.eclipse.edc.policy.engine.spi.plan.step.AtomicConstraintStep.EDC_ATOMIC_CONSTRAINT_STEP_FUNCTION_NAME;
import static org.eclipse.edc.policy.engine.spi.plan.step.AtomicConstraintStep.EDC_ATOMIC_CONSTRAINT_STEP_FUNCTION_PARAMS;
import static org.eclipse.edc.policy.engine.spi.plan.step.AtomicConstraintStep.EDC_ATOMIC_CONSTRAINT_STEP_IS_FILTERED;
import static org.eclipse.edc.policy.engine.spi.plan.step.AtomicConstraintStep.EDC_ATOMIC_CONSTRAINT_STEP_TYPE;
import static org.eclipse.edc.policy.engine.spi.plan.step.MultiplicityConstraintStep.EDC_MULTIPLICITY_CONSTRAINT_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.step.OrConstraintStep.EDC_OR_CONSTRAINT_STEP_TYPE;
import static org.eclipse.edc.policy.engine.spi.plan.step.PermissionStep.EDC_PERMISSION_STEP_DUTY_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.step.PermissionStep.EDC_PERMISSION_STEP_TYPE;
import static org.eclipse.edc.policy.engine.spi.plan.step.RuleStep.EDC_RULE_CONSTRAINT_STEPS;
import static org.eclipse.edc.policy.engine.spi.plan.step.RuleStep.EDC_RULE_FUNCTIONS;
import static org.eclipse.edc.policy.engine.spi.plan.step.RuleStep.EDC_RULE_STEP_FILTERING_REASONS;
import static org.eclipse.edc.policy.engine.spi.plan.step.RuleStep.EDC_RULE_STEP_IS_FILTERED;
import static org.eclipse.edc.policy.engine.spi.plan.step.XoneConstraintStep.EDC_XONE_CONSTRAINT_STEP_TYPE;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonObjectFromPolicyEvaluationPlanTransformerTest {
    
    private final JsonObjectFromPolicyEvaluationPlanTransformer transformer = new JsonObjectFromPolicyEvaluationPlanTransformer(Json.createBuilderFactory(emptyMap()));
    private final TransformerContext context = mock(TransformerContext.class);

    private static AtomicConstraint atomicConstraint(String key, String value) {
        var left = new LiteralExpression(key);
        var right = new LiteralExpression(value);
        return AtomicConstraint.Builder.newInstance()
                .leftExpression(left)
                .operator(EQ)
                .rightExpression(right)
                .build();
    }

    private static AtomicConstraintStep atomicConstraintStep(AtomicConstraint atomicConstraint) {
        AtomicConstraintFunction<Rule> function = mock();
        when(function.name()).thenReturn("AtomicConstraintFunction");
        return new AtomicConstraintStep(atomicConstraint, List.of("filtered constraint"), mock(), function);
    }

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(PolicyEvaluationPlan.class);
        assertThat(transformer.getOutputType()).isEqualTo(JsonObject.class);
    }

    @Test
    void transform_withPermissionStep() {
        var plan = PolicyEvaluationPlan.Builder.newInstance().permission(permissionStep()).build();

        var result = transformer.transform(plan, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonArray(EDC_POLICY_EVALUATION_PLAN_PERMISSION_STEPS)).hasSize(1);
        var permission = result.getJsonArray(EDC_POLICY_EVALUATION_PLAN_PERMISSION_STEPS).get(0).asJsonObject();

        assertThat(permission.getString(TYPE)).isEqualTo(EDC_PERMISSION_STEP_TYPE);
        assertThat(permission.getBoolean(EDC_RULE_STEP_IS_FILTERED)).isEqualTo(true);
        assertThat(permission.getJsonArray(EDC_RULE_STEP_FILTERING_REASONS)).contains(Json.createValue("filter reason"));
        assertThat(permission.getJsonArray(EDC_PERMISSION_STEP_DUTY_STEPS)).hasSize(1);
        assertThat(permission.getJsonArray(EDC_RULE_FUNCTIONS)).hasSize(1).contains(Json.createValue("PermissionFunction"));
        assertThat(permission.getJsonArray(EDC_RULE_CONSTRAINT_STEPS)).hasSize(1);

        var constraint = permission.getJsonArray(EDC_RULE_CONSTRAINT_STEPS).get(0).asJsonObject();

        assertThat(constraint.getString(TYPE)).isEqualTo(EDC_ATOMIC_CONSTRAINT_STEP_TYPE);
        assertThat(constraint.getBoolean(EDC_ATOMIC_CONSTRAINT_STEP_IS_FILTERED)).isTrue();
        assertThat(constraint.getJsonArray(EDC_ATOMIC_CONSTRAINT_STEP_FILTERING_REASONS)).hasSize(1)
                .contains(Json.createValue("filtered constraint"));
        assertThat(constraint.getString(EDC_ATOMIC_CONSTRAINT_STEP_FUNCTION_NAME)).isEqualTo("AtomicConstraintFunction");
        assertThat(constraint.getJsonArray(EDC_ATOMIC_CONSTRAINT_STEP_FUNCTION_PARAMS))
                .hasSize(3)
                .containsExactly(Json.createValue("'foo'"), Json.createValue("EQ"), Json.createValue("'bar'"));

    }

    @Test
    void transform_withValidators() {

        var validator = new ValidatorStep(mock());
        var plan = PolicyEvaluationPlan.Builder.newInstance()
                .preValidator(validator)
                .postValidator(validator)
                .build();

        var result = transformer.transform(plan, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonArray(EDC_POLICY_EVALUATION_PLAN_PRE_VALIDATORS)).hasSize(1);
        assertThat(result.getJsonArray(EDC_POLICY_EVALUATION_PLAN_POST_VALIDATORS)).hasSize(1);

    }

    @ParameterizedTest
    @ArgumentsSource(MultiplicityStepProvider.class)
    void transformWithMultiplicitySteps(PolicyEvaluationPlan plan, String ruleStepProperty, String multiplicityType) {

        var result = transformer.transform(plan, context);

        assertThat(result).isNotNull();

        assertThat(result.getJsonArray(ruleStepProperty)).hasSize(1);
        var rule = result.getJsonArray(ruleStepProperty).get(0).asJsonObject();

        assertThat(rule.getJsonArray(EDC_RULE_CONSTRAINT_STEPS)).hasSize(1);
        var constraint = rule.getJsonArray(EDC_RULE_CONSTRAINT_STEPS).get(0).asJsonObject();

        assertThat(constraint.getString(TYPE)).isEqualTo(multiplicityType);
        assertThat(constraint.getJsonArray(EDC_MULTIPLICITY_CONSTRAINT_STEPS)).hasSize(2);

    }

    private PolicyEvaluationPlan createPlan() {
        return PolicyEvaluationPlan.Builder.newInstance()
                .preValidator(new ValidatorStep(mock()))
                .duty(dutyStep())
                .permission(permissionStep())
                .prohibition(prohibitionStep())
                .postValidator(new ValidatorStep(mock()))
                .build();
    }

    private DutyStep dutyStep() {
        return DutyStep.Builder.newInstance().rule(mock()).filtered(false).build();
    }

    private PermissionStep permissionStep() {
        return permissionStep(atomicConstraintStep(atomicConstraint("foo", "bar")));
    }

    private PermissionStep permissionStep(ConstraintStep constraintStep) {
        RuleFunction<Permission> function = mock();
        when(function.name()).thenReturn("PermissionFunction");
        return PermissionStep.Builder.newInstance()
                .rule(mock())
                .filtered(true)
                .filteringReason("filter reason")
                .ruleFunction(new RuleFunctionStep<>(function, mock()))
                .constraint(constraintStep)
                .dutyStep(DutyStep.Builder.newInstance().rule(mock()).filtered(false).build()).build();
    }

    private ProhibitionStep prohibitionStep() {
        return ProhibitionStep.Builder.newInstance()
                .rule(mock())
                .build();
    }

    private static class MultiplicityStepProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            var firstConstraint = atomicConstraintStep(atomicConstraint("foo", "bar"));
            var secondConstraint = atomicConstraintStep(atomicConstraint("baz", "bar"));

            List<ConstraintStep> constraints = List.of(firstConstraint, secondConstraint);

            var orConstraintStep = new OrConstraintStep(constraints, mock());
            var andConstraintStep = new AndConstraintStep(constraints, mock());
            var xoneConstraintStep = new XoneConstraintStep(constraints, mock());

            var permission = PermissionStep.Builder.newInstance().constraint(orConstraintStep).rule(mock()).build();
            var duty = DutyStep.Builder.newInstance().constraint(xoneConstraintStep).rule(mock()).build();
            var prohibition = ProhibitionStep.Builder.newInstance().constraint(andConstraintStep).rule(mock()).build();

            return Stream.of(
                    of(PolicyEvaluationPlan.Builder.newInstance().permission(permission).build(), EDC_POLICY_EVALUATION_PLAN_PERMISSION_STEPS, EDC_OR_CONSTRAINT_STEP_TYPE),
                    of(PolicyEvaluationPlan.Builder.newInstance().duty(duty).build(), EDC_POLICY_EVALUATION_PLAN_OBLIGATION_STEPS, EDC_XONE_CONSTRAINT_STEP_TYPE),
                    of(PolicyEvaluationPlan.Builder.newInstance().prohibition(prohibition).build(), EDC_POLICY_EVALUATION_PLAN_PROHIBITION_STEPS, EDC_AND_CONSTRAINT_STEP_TYPE)
            );
        }
    }
}
