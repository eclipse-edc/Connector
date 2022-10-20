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

package org.eclipse.edc.protocol.ids.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.Action;
import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.Constraint;
import de.fraunhofer.iais.eis.ConstraintImpl;
import de.fraunhofer.iais.eis.Permission;
import de.fraunhofer.iais.eis.PermissionBuilder;
import de.fraunhofer.iais.eis.util.RdfResource;
import org.eclipse.edc.protocol.ids.jsonld.JsonLd;
import org.eclipse.edc.protocol.ids.jsonld.JsonLdSerializer;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class IdsConstraintTest {
    private ObjectMapper objectMapper;
    private TypeManager typeManager;

    private static final URI ID = URI.create("id");
    private static final String LEFT_OPERAND = "leftOperand";
    private static final BinaryOperator OPERATOR = BinaryOperator.AFTER;
    private static final RdfResource RIGHT_OPERAND = new RdfResource("test", URI.create("http://www.w3.org/2001/XMLSchema#string"));
    private static final URI REFERENCE = URI.create("reference");
    private static final URI UNIT = URI.create("unit");
    private static final URI ENDPOINT = URI.create("endpoint");

    @BeforeEach
    void setUp() {
        typeManager = new TypeManager();
        typeManager.registerContext("ids", JsonLd.getObjectMapper());

        IdsTypeManagerUtil.registerIdsClasses(typeManager);

        objectMapper = typeManager.getMapper("ids");
    }

    void addConstraintConfig() {
        typeManager.registerSerializer("ids", IdsConstraintImpl.class, new JsonLdSerializer<>(IdsConstraintImpl.class, IdsConstants.CONTEXT));
        objectMapper.registerSubtypes(IdsConstraintImpl.class);
        objectMapper = typeManager.getMapper("ids");
    }

    @Test
    void setId() {
        /* ACT */
        var result = new IdsConstraintBuilder(ID).build();

        /* ASSERT */
        var value = result.getId();
        assertThat(value).isNotNull().isEqualTo(ID);
    }

    @Test
    void setLeftOperand() {
        /* ACT */
        var result = (IdsConstraintImpl) new IdsConstraintBuilder().leftOperand(LEFT_OPERAND).build();

        /* ASSERT */
        var value = result.getLeftOperandAsString();
        assertThat(value).isNotNull();
        assertThat(result.getLeftOperand()).isNull();
        assertThat(value).isEqualTo(LEFT_OPERAND);
    }

    @Test
    void setOperator() {
        /* ACT */
        var result = new IdsConstraintBuilder().operator(OPERATOR).build();

        /* ASSERT */
        var value = result.getOperator();
        assertThat(value).isNotNull().isEqualTo(OPERATOR);
    }

    @Test
    void setRightOperand() {
        /* ACT */
        var result = new IdsConstraintBuilder().rightOperand(RIGHT_OPERAND).build();

        /* ASSERT */
        var value = result.getRightOperand();
        assertThat(value).isNotNull().isEqualTo(RIGHT_OPERAND);
    }

    @Test
    void setRightOperandReference() {
        /* ACT */
        var result = new IdsConstraintBuilder().rightOperandReference(REFERENCE).build();

        /* ASSERT */
        var value = result.getRightOperandReference();
        assertThat(value).isNotNull().isEqualTo(REFERENCE);
    }

    @Test
    void setUnit() {
        /* ACT */
        var result = new IdsConstraintBuilder().unit(UNIT).build();

        /* ASSERT */
        var value = result.getUnit();
        assertThat(value).isNotNull().isEqualTo(UNIT);
    }

    @Test
    void setPipEndpoint() {
        /* ACT */
        var result = new IdsConstraintBuilder().pipEndpoint(ENDPOINT).build();

        /* ASSERT */
        var value = result.getPipEndpoint();
        assertThat(value).isNotNull().isEqualTo(ENDPOINT);
    }

    @Test
    void build() {
        /* ACT */
        var result = (IdsConstraintImpl) new IdsConstraintBuilder(ID)
                .leftOperand(LEFT_OPERAND)
                .operator(OPERATOR)
                .rightOperand(RIGHT_OPERAND)
                .rightOperandReference(REFERENCE)
                .unit(UNIT)
                .pipEndpoint(ENDPOINT)
                .build();

        /* ASSERT */
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getLeftOperandAsString()).isEqualTo(LEFT_OPERAND);
        assertThat(result.getOperator()).isEqualTo(OPERATOR);
        assertThat(result.getRightOperand()).isEqualTo(RIGHT_OPERAND);
        assertThat(result.getRightOperandReference()).isEqualTo(REFERENCE);
        assertThat(result.getUnit()).isEqualTo(UNIT);
        assertThat(result.getPipEndpoint()).isEqualTo(ENDPOINT);
    }

    @Test
    void serialize() throws JsonProcessingException {
        /* ARRANGE */
        var constraint = (IdsConstraintImpl) new IdsConstraintBuilder(ID)
                .leftOperand(LEFT_OPERAND)
                .operator(OPERATOR)
                .rightOperand(RIGHT_OPERAND)
                .rightOperandReference(REFERENCE)
                .unit(UNIT)
                .pipEndpoint(ENDPOINT)
                .build();

        /* ACT */
        var result = objectMapper.writeValueAsString(constraint);

        /* ASSERT */
        assertThat(result).isNotBlank();
    }

    @Test
    void failedConstraintDeserialization_leftOperand_isNull() throws JsonProcessingException {
        /* ARRANGE */
        var constraint = new IdsConstraintBuilder(ID)
                .leftOperand(LEFT_OPERAND)
                .operator(OPERATOR)
                .rightOperand(RIGHT_OPERAND)
                .rightOperandReference(REFERENCE)
                .unit(UNIT)
                .pipEndpoint(ENDPOINT)
                .build();

        var constraintAsString = objectMapper.writeValueAsString(constraint);

        /* ACT */
        var result = objectMapper.readValue(constraintAsString, Constraint.class);

        /* ASSERT */
        assertThat(result).isNotNull().isInstanceOf(ConstraintImpl.class);
        assertThat(result.getLeftOperand()).isNull();
    }

    @Test
    void deserializeConstraintWithRightOperand() throws JsonProcessingException {
        /* ARRANGE */
        var constraint = (IdsConstraintImpl) new IdsConstraintBuilder(ID)
                .leftOperand(LEFT_OPERAND)
                .operator(OPERATOR)
                .rightOperand(RIGHT_OPERAND)
                .unit(UNIT)
                .pipEndpoint(ENDPOINT)
                .build();

        addConstraintConfig();

        var constraintAsString = objectMapper.writeValueAsString(constraint);

        /* ACT */
        var result = objectMapper.readValue(constraintAsString, Constraint.class);

        /* ASSERT */
        assertThat(result).isNotNull().isInstanceOf(IdsConstraintImpl.class);
        assertThat(result.getLeftOperand()).isNull();
        assertThat(result.getId()).isEqualTo(ID);
        assertThat(((IdsConstraintImpl) result).getLeftOperandAsString()).isEqualTo(LEFT_OPERAND);
        assertThat(result.getOperator()).isEqualTo(OPERATOR);
        assertThat(result.getPipEndpoint()).isEqualTo(ENDPOINT);
        assertThat(result.getRightOperand().getType()).isEqualTo(RIGHT_OPERAND.getType());
        assertThat(result.getRightOperand().getValue()).isEqualTo(RIGHT_OPERAND.getValue());
        assertThat(result.getRightOperandReference()).isNull();
        assertThat(result.getUnit()).isEqualTo(UNIT);
    }

    @Test
    void deserializeConstraintWithRightOperandReference() throws JsonProcessingException {
        /* ARRANGE */
        var constraint = (IdsConstraintImpl) new IdsConstraintBuilder(ID)
                .leftOperand(LEFT_OPERAND)
                .operator(OPERATOR)
                .rightOperandReference(REFERENCE)
                .unit(UNIT)
                .pipEndpoint(ENDPOINT)
                .build();

        addConstraintConfig();

        var constraintAsString = objectMapper.writeValueAsString(constraint);

        /* ACT */
        var result = objectMapper.readValue(constraintAsString, Constraint.class);

        /* ASSERT */
        assertThat(result).isNotNull().isInstanceOf(IdsConstraintImpl.class);
        assertThat(result.getLeftOperand()).isNull();
        assertThat(result.getId()).isEqualTo(ID);
        assertThat(((IdsConstraintImpl) result).getLeftOperandAsString()).isEqualTo(LEFT_OPERAND);
        assertThat(result.getOperator()).isEqualTo(OPERATOR);
        assertThat(result.getPipEndpoint()).isEqualTo(ENDPOINT);
        assertThat(result.getRightOperand()).isNull();
        assertThat(result.getRightOperandReference()).isEqualTo(REFERENCE);
        assertThat(result.getUnit()).isEqualTo(UNIT);
    }

    /**
     * Ensure proper {@link Constraint} mapping even if optional properties "unit" and "pipEndpoint" are not provided.
     */
    @Test
    void deserializeConstraintWithoutOptionalFields() throws JsonProcessingException {
        /* ARRANGE */
        var constraint = (IdsConstraintImpl) new IdsConstraintBuilder(ID)
                .leftOperand(LEFT_OPERAND)
                .operator(OPERATOR)
                .rightOperand(RIGHT_OPERAND)
                .build();

        addConstraintConfig();

        var constraintAsString = objectMapper.writeValueAsString(constraint);

        /* ACT */
        var result = objectMapper.readValue(constraintAsString, Constraint.class);

        /* ASSERT */
        assertThat(result).isNotNull().isInstanceOf(IdsConstraintImpl.class);
        assertThat(result.getLeftOperand()).isNull();
        assertThat(result.getId()).isEqualTo(ID);
        assertThat(((IdsConstraintImpl) result).getLeftOperandAsString()).isEqualTo(LEFT_OPERAND);
        assertThat(result.getOperator()).isEqualTo(OPERATOR);
        assertThat(result.getRightOperand().getType()).isEqualTo(RIGHT_OPERAND.getType());
        assertThat(result.getRightOperand().getValue()).isEqualTo(RIGHT_OPERAND.getValue());
        assertThat(result.getUnit()).isNull();
        assertThat(result.getPipEndpoint()).isNull();
    }


    @Test
    void deserializePermission() throws JsonProcessingException {
        /* ARRANGE */
        var constraint = (IdsConstraintImpl) new IdsConstraintBuilder(ID)
                .leftOperand(LEFT_OPERAND)
                .operator(OPERATOR)
                .rightOperand(RIGHT_OPERAND)
                .rightOperandReference(REFERENCE)
                .unit(UNIT)
                .pipEndpoint(ENDPOINT)
                .build();
        var permission = new PermissionBuilder()
                ._action_(Action.USE)
                ._constraint_(constraint)
                ._assignee_(URI.create("assignee"))
                ._assigner_(URI.create("assigner"))
                ._target_(URI.create("target"))
                .build();

        addConstraintConfig();

        var permissionAsString = objectMapper.writeValueAsString(permission);

        /* ACT */
        var result = objectMapper.readValue(permissionAsString, Permission.class);

        /* ASSERT */
        assertThat(result).isNotNull();
        assertThat(result.getConstraint()).isNotNull();

        var resultConstraint = (Constraint) result.getConstraint().get(0);
        assertThat(resultConstraint).isNotNull().isInstanceOf(IdsConstraintImpl.class);
        assertThat(resultConstraint.getLeftOperand()).isNull();
        assertThat(resultConstraint.getId()).isEqualTo(ID);
        assertThat(((IdsConstraintImpl) resultConstraint).getLeftOperandAsString()).isEqualTo(LEFT_OPERAND);
        assertThat(resultConstraint.getOperator()).isEqualTo(OPERATOR);
        assertThat(resultConstraint.getPipEndpoint()).isEqualTo(ENDPOINT);
        assertThat(resultConstraint.getRightOperand().getType()).isEqualTo(RIGHT_OPERAND.getType());
        assertThat(resultConstraint.getRightOperand().getValue()).isEqualTo(RIGHT_OPERAND.getValue());
        assertThat(resultConstraint.getRightOperandReference()).isNull(); // reference is ignored when right operand is provided
        assertThat(resultConstraint.getUnit()).isEqualTo(UNIT);
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideInvalidConstraints")
    void deserializeFailureIfIncorrectInput_shouldThrowEdcException(String name, Constraint constraint) throws JsonProcessingException {
        /* ARRANGE */
        var idsConstraint = (IdsConstraintImpl) constraint;

        addConstraintConfig();

        var constraintAsString = objectMapper.writeValueAsString(idsConstraint);

        /* ASSERT */
        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> objectMapper.readValue(constraintAsString, Constraint.class));
    }

    private static Stream<Arguments> provideInvalidConstraints() {
        var noLeftOperand = (IdsConstraintImpl) new IdsConstraintBuilder(ID)
                .operator(OPERATOR)
                .rightOperand(RIGHT_OPERAND)
                .build();

        var noOperator = (IdsConstraintImpl) new IdsConstraintBuilder(ID)
                .leftOperand(LEFT_OPERAND)
                .rightOperand(RIGHT_OPERAND)
                .build();

        var noRightOperandOrReference = (IdsConstraintImpl) new IdsConstraintBuilder(ID)
                .leftOperand(LEFT_OPERAND)
                .operator(OPERATOR)
                .build();

        return Stream.of(
                Arguments.of("NO LEFT OPERAND", noLeftOperand),
                Arguments.of("NO OPERATOR", noOperator),
                Arguments.of("NO RIGHT OPERAND OR REFERENCE", noRightOperandOrReference)
        );
    }
}
