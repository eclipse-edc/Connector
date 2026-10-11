/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.api.management.policy.validation;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.StringReader;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_POLICY;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OR_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_SET;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROFILE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_SCHEMA;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class PolicyDefinitionValidatorTest {

    private final Validator<JsonObject> validator = PolicyDefinitionValidator.instance();

    @Test
    void shouldSucceed_whenObjectIsValid() {
        var policyDefinition = createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder()
                        .add(createObjectBuilder().add(TYPE, createArrayBuilder().add(ODRL_POLICY_TYPE_SET))))
                .build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenIdIsBlank() {
        var policyDefinition = createObjectBuilder()
                .add(ID, " ")
                .build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> ID.equals(it.path()))
                .anySatisfy(violation -> assertThat(violation.message()).contains("blank"));
    }

    @Test
    void shouldFail_whenPolicyIsMissing() {
        var policyDefinition = createObjectBuilder().build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> EDC_POLICY_DEFINITION_POLICY.equals(it.path()))
                .anySatisfy(violation -> assertThat(violation.message()).contains("mandatory"));
    }


    @Test
    void shouldFail_whenTypeIsMissing() {
        var policyDefinition = createObjectBuilder().build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> EDC_POLICY_DEFINITION_POLICY.equals(it.path()))
                .anySatisfy(violation -> assertThat(violation.message()).contains("mandatory"));
    }

    @Test
    void shouldFail_whenTypeIsInvalid() {
        var policyDefinition = createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder()
                        .add(createObjectBuilder().add(TYPE, createArrayBuilder().add("InvalidType"))))
                .build();
        var result = validator.validate(policyDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> it.path().contains(EDC_POLICY_DEFINITION_POLICY))
                .anySatisfy(violation -> assertThat(violation.message()).contains("was expected to be"));
    }

    @Test
    void shouldSucceed_whenPolicyWithPermissionIsValid() {
        var policy = createObjectBuilder()
                .add(TYPE, policySet())
                .add(ODRL_ACTION_ATTRIBUTE, createValidAction())
                .add(ODRL_PERMISSION_ATTRIBUTE, createValidPermission());
        var policyDefinition = createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder().add(policy))
                .build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenPermissionActionIsMissing() {
        var permission = createArrayBuilder().add(createObjectBuilder()
                .add(ODRL_CONSTRAINT_ATTRIBUTE, createValidConstraint("GroupNumber", "isPartOf", "allowedGroups")));
        var policyDefinition = createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder().add(createObjectBuilder()
                        .add(TYPE, policySet())
                        .add(ODRL_PERMISSION_ATTRIBUTE, permission))
                )
                .build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> it.path().contains(ODRL_ACTION_ATTRIBUTE))
                .anySatisfy(violation -> assertThat(violation.message()).contains("mandatory"));
    }

    @Test
    void shouldSucceed_whenPermissionConstraintIsMissing() {
        var constraint = createArrayBuilder();
        var permission = createArrayBuilder().add(createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, createValidAction())
                .add(ODRL_CONSTRAINT_ATTRIBUTE, constraint));
        var policy = createObjectBuilder()
                .add(TYPE, policySet())
                .add(ODRL_PERMISSION_ATTRIBUTE, permission);
        var policyDefinition = createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder().add(policy))
                .build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenConstraintOperatorIsMissing() {
        var constraint = createArrayBuilder().add(createObjectBuilder());
        var permission = createArrayBuilder().add(createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, createValidAction())
                .add(ODRL_CONSTRAINT_ATTRIBUTE, constraint));
        var policy = createObjectBuilder()
                .add(TYPE, policySet())
                .add(ODRL_PERMISSION_ATTRIBUTE, permission);
        var policyDefinition = createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder().add(policy))
                .build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> ODRL_LEFT_OPERAND_ATTRIBUTE.equals(it.path()))
                .anySatisfy(violation -> assertThat(violation.message()).contains("mandatory"));
    }

    @Test
    void shouldFail_whenOperatorIsNotValid() {
        var constraint = createValidConstraint("left", "unsupported-operator", "right");
        var permission = createArrayBuilder().add(createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, createValidAction())
                .add(ODRL_CONSTRAINT_ATTRIBUTE, constraint));
        var policy = createObjectBuilder()
                .add(TYPE, policySet())
                .add(ODRL_PERMISSION_ATTRIBUTE, permission);
        var policyDefinition = createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder().add(policy))
                .build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> it.path().contains(ODRL_OPERATOR_ATTRIBUTE))
                .anySatisfy(violation -> assertThat(violation.message()).contains("unsupported-operator"));
    }

    @Test
    void shouldFail_whenRightOperandIsIdReference() {
        var result = validator.validate(policyDefinitionWithRightOperand("[{\"@id\": \"https://w3id.org/example/EU\"}]"));

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .anySatisfy(violation -> assertThat(violation.message()).contains("use the plain string"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[{\"@value\": \"\"}]",
            "[{\"@value\": \" \"}]",
            "[{\"@value\": \"a\"}, {\"@value\": \" \"}]"
    })
    void shouldFail_whenRightOperandIsBlank(String rightOperand) {
        var result = validator.validate(policyDefinitionWithRightOperand(rightOperand));

        assertThat(result).isFailed();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[{\"@value\": \"EU\"}]",
            "[{\"@value\": 5}]",
            "[{\"@value\": true}]",
            "[{\"@value\": \"2025-01-01\", \"@type\": \"http://www.w3.org/2001/XMLSchema#date\"}]",
            "[{\"@value\": \"a\"}, {\"@value\": 5}]"
    })
    void shouldSucceed_whenRightOperandIsLiteral(String rightOperand) {
        var result = validator.validate(policyDefinitionWithRightOperand(rightOperand));

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldSucceed_whenLogicalConstraintIsPresent() {
        var permission = createArrayBuilder().add(createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, createValidAction())
                .add(ODRL_CONSTRAINT_ATTRIBUTE, createArrayBuilder().add(createObjectBuilder()
                        .add(ODRL_AND_CONSTRAINT_ATTRIBUTE, createArrayBuilder()
                                .add(createValidConstraint("inForceDate", "gteq", "2024-01-01T00:00:00Z"))
                                .add(createValidConstraint("inForceDate", "lteq", "2024-04-01T00:00:00Z"))))));
        var policy = createObjectBuilder()
                .add(TYPE, policySet())
                .add(ODRL_PERMISSION_ATTRIBUTE, permission);
        var policyDefinition = createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder().add(policy))
                .build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenLogicalConstraintWithMultipleOperandsIsPresent() {
        var permission = createArrayBuilder().add(createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, createValidAction())
                .add(ODRL_CONSTRAINT_ATTRIBUTE, createArrayBuilder().add(createObjectBuilder()
                        .add(ODRL_AND_CONSTRAINT_ATTRIBUTE, createArrayBuilder())
                        .add(ODRL_OR_CONSTRAINT_ATTRIBUTE, createArrayBuilder())
                )));
        var policy = createObjectBuilder()
                .add(TYPE, policySet())
                .add(ODRL_PERMISSION_ATTRIBUTE, permission);
        var policyDefinition = createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder().add(policy))
                .build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .hasSize(1)
                .anySatisfy(violation -> {
                    assertThat(violation.message()).contains("multiple operands");
                    assertThat(violation.path()).endsWith(ODRL_CONSTRAINT_ATTRIBUTE);
                });
    }

    @Test
    void shouldSucceed_whenProfileIsPresent() {
        var policy = createObjectBuilder()
                .add(TYPE, policySet())
                .add(ODRL_PROFILE_ATTRIBUTE, createArrayBuilder().add(createObjectBuilder().add(ID, "value1")));
        var policyDefinition = createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder().add(policy))
                .build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenProfileIsWrongType() {
        var policy = createObjectBuilder()
                .add(TYPE, policySet())
                .add(ODRL_PROFILE_ATTRIBUTE, createArrayBuilder().add(createObjectBuilder().add(ID, createObjectBuilder())));
        var policyDefinition = createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder().add(policy))
                .build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> ODRL_PROFILE_ATTRIBUTE.equals(it.path()))
                .anySatisfy(violation -> assertThat(violation.message()).contains("type"));
    }

    @Test
    void shouldFail_whenProfileIsEmptyArray() {
        var policy = createObjectBuilder()
                .add(TYPE, policySet())
                .add(ODRL_PROFILE_ATTRIBUTE, createArrayBuilder());
        var policyDefinition = createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder().add(policy))
                .build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> ODRL_PROFILE_ATTRIBUTE.equals(it.path()))
                .anySatisfy(violation -> assertThat(violation.message()).contains("elements"));
    }

    @Test
    void shouldFail_whenProfileIsBlank() {
        var policy = createObjectBuilder()
                .add(TYPE, policySet())
                .add(ODRL_PROFILE_ATTRIBUTE, createArrayBuilder().add(createObjectBuilder().add(ID, " ")));
        var policyDefinition = createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder().add(policy))
                .build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> ODRL_PROFILE_ATTRIBUTE.equals(it.path()))
                .anySatisfy(violation -> assertThat(violation.message()).contains("blank"));
    }

    @Test
    void shouldFail_whenProfileIsEmpty() {
        var policy = createObjectBuilder()
                .add(TYPE, policySet())
                .add(ODRL_PROFILE_ATTRIBUTE, createArrayBuilder().add(createObjectBuilder().add(ID, "")));
        var policyDefinition = createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder().add(policy))
                .build();

        var result = validator.validate(policyDefinition);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .filteredOn(it -> ODRL_PROFILE_ATTRIBUTE.equals(it.path()))
                .anySatisfy(violation -> assertThat(violation.message()).contains("empty"));
    }

    private JsonObject policyDefinitionWithRightOperand(String rightOperand) {
        var constraint = createObjectBuilder()
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, createArrayBuilder().add(createObjectBuilder().add(ID, "left")))
                .add(ODRL_OPERATOR_ATTRIBUTE, createArrayBuilder().add(createObjectBuilder().add(ID, ODRL_SCHEMA + "eq")))
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, Json.createReader(new StringReader(rightOperand)).readArray());
        var permission = createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, createValidAction())
                .add(ODRL_CONSTRAINT_ATTRIBUTE, createArrayBuilder().add(constraint));
        var policy = createObjectBuilder()
                .add(TYPE, policySet())
                .add(ODRL_PERMISSION_ATTRIBUTE, createArrayBuilder().add(permission));
        return createObjectBuilder()
                .add(EDC_POLICY_DEFINITION_POLICY, createArrayBuilder().add(policy))
                .build();
    }

    private JsonArrayBuilder policySet() {
        return createArrayBuilder().add(ODRL_POLICY_TYPE_SET);
    }

    private JsonArrayBuilder createValidPermission() {
        return createArrayBuilder().add(createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, createValidAction())
                .add(ODRL_CONSTRAINT_ATTRIBUTE, createValidConstraint("GroupNumber", ODRL_SCHEMA + "isPartOf", "allowedGroups")));
    }

    private JsonArrayBuilder createValidAction() {
        return createArrayBuilder().add(createObjectBuilder().add(ID, "use"));
    }

    private JsonArrayBuilder createValidConstraint(String left, String operator, String right) {
        var leftOperand = createArrayBuilder().add(createObjectBuilder().add(ID, left));
        var operatorObject = createArrayBuilder().add(createObjectBuilder().add(ID, operator));
        var rightOperand = createArrayBuilder().add(createObjectBuilder().add(VALUE, right));
        return createArrayBuilder().add(createObjectBuilder()
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, leftOperand)
                .add(ODRL_OPERATOR_ATTRIBUTE, operatorObject)
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, rightOperand));
    }



}
