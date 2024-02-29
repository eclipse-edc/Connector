/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.edc.core.transform.transformer.from;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_TYPE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ASSIGNEE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ASSIGNER_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSEQUENCE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_DUTY_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_INCLUDED_IN_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OR_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_AGREEMENT;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_OFFER;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_SET;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_REFINEMENT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_XONE_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.policy.model.PolicyType.CONTRACT;
import static org.eclipse.edc.policy.model.PolicyType.OFFER;
import static org.eclipse.edc.policy.model.PolicyType.SET;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectFromPolicyTransformerTest {
    
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);
    
    private JsonObjectFromPolicyTransformer transformer;
    
    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromPolicyTransformer(jsonFactory);
    }
    
    @Test
    void transform_policyWithAllRuleTypes_returnJsonObject() {
        var action = Action.Builder.newInstance().type("use").build();
        var permission = Permission.Builder.newInstance().action(action).build();
        var prohibition = Prohibition.Builder.newInstance().action(action).build();
        var duty = Duty.Builder.newInstance().action(action).build();
        var policy = Policy.Builder.newInstance()
                .target("target")
                .assignee("assignee")
                .assigner("assigner")
                .permission(permission)
                .prohibition(prohibition)
                .duty(duty)
                .build();
        
        var result = transformer.transform(policy, context);
        
        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(ODRL_POLICY_TYPE_SET);
        assertThat(result.get(ODRL_TARGET_ATTRIBUTE))
                .isNotNull()
                .isInstanceOf(JsonArray.class)
                .extracting(JsonValue::asJsonArray)
                .matches(it -> !it.isEmpty())
                .asInstanceOf(LIST).first()
                .asInstanceOf(type(JsonObject.class))
                .matches(it -> it.getString(ID).equals("target"));

        assertThat(result.getString(ODRL_ASSIGNEE_ATTRIBUTE)).isEqualTo("assignee");
        assertThat(result.getString(ODRL_ASSIGNER_ATTRIBUTE)).isEqualTo("assigner");

        assertThat(result.get(ODRL_PERMISSION_ATTRIBUTE))
                .isNotNull()
                .isInstanceOf(JsonArray.class)
                .matches(v -> v.asJsonArray().size() == 1);
        assertThat(result.get(ODRL_PROHIBITION_ATTRIBUTE))
                .isNotNull()
                .isInstanceOf(JsonArray.class)
                .matches(v -> v.asJsonArray().size() == 1);
        assertThat(result.get(ODRL_OBLIGATION_ATTRIBUTE))
                .isNotNull()
                .isInstanceOf(JsonArray.class)
                .matches(v -> v.asJsonArray().size() == 1);
        
        var permissionJson = result.get(ODRL_PERMISSION_ATTRIBUTE).asJsonArray().get(0).asJsonObject();
        assertThat(permissionJson.getJsonObject(ODRL_ACTION_ATTRIBUTE)).isNotNull();
        assertThat(permissionJson.getJsonObject(ODRL_CONSTRAINT_ATTRIBUTE)).isNull();
    
        var prohibitionJson = result.get(ODRL_PROHIBITION_ATTRIBUTE).asJsonArray().get(0).asJsonObject();
        assertThat(prohibitionJson.getJsonObject(ODRL_ACTION_ATTRIBUTE)).isNotNull();
        assertThat(prohibitionJson.getJsonObject(ODRL_CONSTRAINT_ATTRIBUTE)).isNull();
    
        var dutyJson = result.get(ODRL_OBLIGATION_ATTRIBUTE).asJsonArray().get(0).asJsonObject();
        assertThat(dutyJson.getJsonObject(ODRL_ACTION_ATTRIBUTE)).isNotNull();
        assertThat(dutyJson.getJsonObject(ODRL_CONSTRAINT_ATTRIBUTE)).isNull();
        
        verify(context, never()).reportProblem(anyString());
    }
    
    @Test
    void transform_actionWithAllAttributes_returnJsonObject() {
        var constraint = getConstraint();
        var action = Action.Builder.newInstance()
                .type("use")
                .includedIn("includedIn")
                .constraint(constraint)
                .build();
        var permission = Permission.Builder.newInstance().action(action).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();
    
        var result = transformer.transform(policy, context);
    
        var permissionJson = result.get(ODRL_PERMISSION_ATTRIBUTE).asJsonArray().get(0).asJsonObject();
        assertThat(permissionJson.getJsonObject(ODRL_ACTION_ATTRIBUTE)).isNotNull();
        
        var actionJson = permissionJson.getJsonObject(ODRL_ACTION_ATTRIBUTE);
        assertThat(actionJson.getJsonString(ODRL_ACTION_TYPE_ATTRIBUTE).getString()).isEqualTo(action.getType());
        assertThat(actionJson.getJsonString(ODRL_INCLUDED_IN_ATTRIBUTE).getString()).isEqualTo(action.getIncludedIn());
        assertThat(actionJson.getJsonObject(ODRL_REFINEMENT_ATTRIBUTE)).isNotNull();
        
        var constraintJson = actionJson.getJsonObject(ODRL_REFINEMENT_ATTRIBUTE);
        assertThat(constraintJson.getJsonObject(ODRL_LEFT_OPERAND_ATTRIBUTE).getJsonString(VALUE).getString())
                .isEqualTo(((LiteralExpression) constraint.getLeftExpression()).getValue());
        assertThat(constraintJson.getJsonArray(ODRL_OPERATOR_ATTRIBUTE).getJsonObject(0).getString(ID))
                .isEqualTo(constraint.getOperator().getOdrlRepresentation());
        assertThat(constraintJson.getJsonObject(ODRL_RIGHT_OPERAND_ATTRIBUTE).getJsonString(VALUE).getString())
                .isEqualTo(((LiteralExpression) constraint.getRightExpression()).getValue());
    
        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_actionNull_returnJsonObject() {
        var permission = Permission.Builder.newInstance().action(null).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();

        var result = transformer.transform(policy, context);

        var permissionJson = result.get(ODRL_PERMISSION_ATTRIBUTE).asJsonArray().get(0).asJsonObject();
        assertThat(permissionJson.get(ODRL_ACTION_ATTRIBUTE)).isNotNull();
    }
    
    @Test
    void transform_permissionWithConstraintAndDuty_returnJsonObject() {
        var constraint = getConstraint();
        var action = getAction();
        var duty = Duty.Builder.newInstance().action(action).build();
        var permission = Permission.Builder.newInstance()
                .action(action)
                .constraint(constraint)
                .duty(duty)
                .build();
        var policy = Policy.Builder.newInstance().permission(permission).build();
    
        var result = transformer.transform(policy, context);
    
        var permissionJson = result.get(ODRL_PERMISSION_ATTRIBUTE).asJsonArray().get(0).asJsonObject();
        assertThat(permissionJson.getJsonArray(ODRL_CONSTRAINT_ATTRIBUTE)).hasSize(1);
        assertThat(permissionJson.getJsonArray(ODRL_DUTY_ATTRIBUTE)).hasSize(1);
        
        var constraintJson = permissionJson.getJsonArray(ODRL_CONSTRAINT_ATTRIBUTE).get(0).asJsonObject();
        assertThat(constraintJson.getJsonObject(ODRL_LEFT_OPERAND_ATTRIBUTE).getJsonString(VALUE).getString())
                .isEqualTo(((LiteralExpression) constraint.getLeftExpression()).getValue());
        assertThat(constraintJson.getJsonArray(ODRL_OPERATOR_ATTRIBUTE).getJsonObject(0).getString(ID))
                .isEqualTo(constraint.getOperator().getOdrlRepresentation());
        assertThat(constraintJson.getJsonObject(ODRL_RIGHT_OPERAND_ATTRIBUTE).getJsonString(VALUE).getString())
                .isEqualTo(((LiteralExpression) constraint.getRightExpression()).getValue());

        var dutyJson = permissionJson.getJsonArray(ODRL_DUTY_ATTRIBUTE).get(0).asJsonObject();
        assertThat(dutyJson.getJsonObject(ODRL_ACTION_ATTRIBUTE)).isNotNull();
    
        verify(context, never()).reportProblem(anyString());
    }
    
    @Test
    void transform_prohibitionWithConstraint_returnJsonObject() {
        var constraint = getConstraint();
        var action = getAction();
        var prohibition = Prohibition.Builder.newInstance()
                .action(action)
                .constraint(constraint)
                .build();
        var policy = Policy.Builder.newInstance().prohibition(prohibition).build();
        
        var result = transformer.transform(policy, context);
        
        var prohibitionJson = result.get(ODRL_PROHIBITION_ATTRIBUTE).asJsonArray().get(0).asJsonObject();
        assertThat(prohibitionJson.getJsonArray(ODRL_CONSTRAINT_ATTRIBUTE)).hasSize(1);
        
        var constraintJson = prohibitionJson.getJsonArray(ODRL_CONSTRAINT_ATTRIBUTE).get(0).asJsonObject();
        assertThat(constraintJson.getJsonObject(ODRL_LEFT_OPERAND_ATTRIBUTE).getJsonString(VALUE).getString())
                .isEqualTo(((LiteralExpression) constraint.getLeftExpression()).getValue());
        assertThat(constraintJson.getJsonArray(ODRL_OPERATOR_ATTRIBUTE).getJsonObject(0).getString(ID))
                .isEqualTo(constraint.getOperator().getOdrlRepresentation());
        assertThat(constraintJson.getJsonObject(ODRL_RIGHT_OPERAND_ATTRIBUTE).getJsonString(VALUE).getString())
                .isEqualTo(((LiteralExpression) constraint.getRightExpression()).getValue());
    
        verify(context, never()).reportProblem(anyString());
    }
    
    @Test
    void transform_dutyWithConstraintAndConsequence_returnJsonObject() {
        var constraint = getConstraint();
        var action = getAction();
        var consequence = Duty.Builder.newInstance().action(action).build();
        var duty = Duty.Builder.newInstance()
                .action(action)
                .constraint(constraint)
                .consequence(consequence)
                .build();
        var policy = Policy.Builder.newInstance().duty(duty).build();
        
        var result = transformer.transform(policy, context);
        
        var dutyJson = result.get(ODRL_OBLIGATION_ATTRIBUTE).asJsonArray().get(0).asJsonObject();
        assertThat(dutyJson.getJsonArray(ODRL_CONSTRAINT_ATTRIBUTE)).hasSize(1);
        
        var constraintJson = dutyJson.getJsonArray(ODRL_CONSTRAINT_ATTRIBUTE).get(0).asJsonObject();
        assertThat(constraintJson.getJsonObject(ODRL_LEFT_OPERAND_ATTRIBUTE).getJsonString(VALUE).getString())
                .isEqualTo(((LiteralExpression) constraint.getLeftExpression()).getValue());
        assertThat(constraintJson.getJsonArray(ODRL_OPERATOR_ATTRIBUTE).getJsonObject(0).getString(ID))
                .isEqualTo(constraint.getOperator().getOdrlRepresentation());
        assertThat(constraintJson.getJsonObject(ODRL_RIGHT_OPERAND_ATTRIBUTE).getJsonString(VALUE).getString())
                .isEqualTo(((LiteralExpression) constraint.getRightExpression()).getValue());
        
        var consequenceJson = dutyJson.getJsonObject(ODRL_CONSEQUENCE_ATTRIBUTE);
        assertThat(consequenceJson.getJsonObject(ODRL_ACTION_ATTRIBUTE)).isNotNull();
    
        verify(context, never()).reportProblem(anyString());
    }
    
    @Test
    void transform_andConstraint_returnJsonObject() {
        var constraint = getConstraint();
        var andConstraint = AndConstraint.Builder.newInstance().constraint(constraint).build();
        var permission = Permission.Builder.newInstance()
                .action(getAction())
                .constraint(andConstraint)
                .build();
        var policy = Policy.Builder.newInstance().permission(permission).build();
    
        var result = transformer.transform(policy, context);
    
        var permissionJson = result.get(ODRL_PERMISSION_ATTRIBUTE).asJsonArray().get(0).asJsonObject();
        assertThat(permissionJson.getJsonArray(ODRL_CONSTRAINT_ATTRIBUTE))
                .isNotNull()
                .hasSize(1);
        
        var constraintJson = permissionJson.getJsonArray(ODRL_CONSTRAINT_ATTRIBUTE).get(0).asJsonObject();
        assertThat(constraintJson.getJsonArray(ODRL_AND_CONSTRAINT_ATTRIBUTE)).hasSize(1);
    
        verify(context, never()).reportProblem(anyString());
    }
    
    @Test
    void transform_orConstraint_returnJsonObject() {
        var constraint = getConstraint();
        var orConstraint = OrConstraint.Builder.newInstance().constraint(constraint).build();
        var permission = Permission.Builder.newInstance()
                .action(getAction())
                .constraint(orConstraint)
                .build();
        var policy = Policy.Builder.newInstance().permission(permission).build();
        
        var result = transformer.transform(policy, context);
        
        var permissionJson = result.get(ODRL_PERMISSION_ATTRIBUTE).asJsonArray().get(0).asJsonObject();
        assertThat(permissionJson.getJsonArray(ODRL_CONSTRAINT_ATTRIBUTE))
                .isNotNull()
                .hasSize(1);

        var constraintJson = permissionJson.getJsonArray(ODRL_CONSTRAINT_ATTRIBUTE).get(0).asJsonObject();
        assertThat(constraintJson.getJsonArray(ODRL_OR_CONSTRAINT_ATTRIBUTE)).hasSize(1);
    
        verify(context, never()).reportProblem(anyString());
    }
    
    @Test
    void transform_xoneConstraint_returnJsonObject() {
        var constraint = getConstraint();
        var xoneConstraint = XoneConstraint.Builder.newInstance().constraint(constraint).build();
        var permission = Permission.Builder.newInstance()
                .action(getAction())
                .constraint(xoneConstraint)
                .build();
        var policy = Policy.Builder.newInstance().permission(permission).build();
        
        var result = transformer.transform(policy, context);
        
        var permissionJson = result.get(ODRL_PERMISSION_ATTRIBUTE).asJsonArray().get(0).asJsonObject();
        assertThat(permissionJson.getJsonArray(ODRL_CONSTRAINT_ATTRIBUTE))
                .isNotNull()
                .hasSize(1);

        var constraintJson = permissionJson.getJsonArray(ODRL_CONSTRAINT_ATTRIBUTE).get(0).asJsonObject();
        assertThat(constraintJson.getJsonArray(ODRL_XONE_CONSTRAINT_ATTRIBUTE)).hasSize(1);
    
        verify(context, never()).reportProblem(anyString());
    }

    @ParameterizedTest
    @ArgumentsSource(PolicyTypeToOdrl.class)
    void shouldMapPolicyTypeToOdrlType(PolicyType type, String expectedType) {
        var policy = Policy.Builder.newInstance().type(type).build();

        var result = transformer.transform(policy, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(TYPE)).isEqualTo(expectedType);
    }

    private static class PolicyTypeToOdrl implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    arguments(SET, ODRL_POLICY_TYPE_SET),
                    arguments(OFFER, ODRL_POLICY_TYPE_OFFER),
                    arguments(CONTRACT, ODRL_POLICY_TYPE_AGREEMENT)
            );
        }
    }

    private Action getAction() {
        return Action.Builder.newInstance().type("use").build();
    }
    
    private AtomicConstraint getConstraint() {
        return AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("left"))
                .operator(Operator.EQ)
                .rightExpression(new LiteralExpression("right"))
                .build();
    }
}
