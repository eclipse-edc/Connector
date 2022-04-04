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

package org.eclipse.dataspaceconnector.ids.core.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.Action;
import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.Constraint;
import de.fraunhofer.iais.eis.ConstraintImpl;
import de.fraunhofer.iais.eis.Permission;
import de.fraunhofer.iais.eis.PermissionBuilder;
import de.fraunhofer.iais.eis.util.RdfResource;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdsConstraintTest {

    URI id = URI.create("id");
    String leftOperand = "leftOperand";
    BinaryOperator operator = BinaryOperator.AFTER;
    RdfResource rightOperand = new RdfResource("test", URI.create("http://www.w3.org/2001/XMLSchema#string"));
    URI reference = URI.create("reference");
    URI unit = URI.create("unit");
    URI endpoint = URI.create("endpoint");

    @Test
    void setId() {
        /* ACT */
        var result = new IdsConstraintBuilder(id).build();

        /* ASSERT */
        var value = result.getId();
        assertNotNull(value);
        assertEquals(id, value);
    }

    @Test
    void setLeftOperand() {
        /* ACT */
        var result = (IdsConstraintImpl) new IdsConstraintBuilder().leftOperand(leftOperand).build();

        /* ASSERT */
        var value = result.getLeftOperandAsString();
        assertNotNull(value);
        assertNull(result.getLeftOperand());
        assertEquals(leftOperand, value);
    }

    @Test
    void setOperator() {
        /* ACT */
        var result = new IdsConstraintBuilder().operator(operator).build();

        /* ASSERT */
        var value = result.getOperator();
        assertNotNull(value);
        assertEquals(operator, value);
    }

    @Test
    void setRightOperand() {
        /* ACT */
        var result = new IdsConstraintBuilder().rightOperand(rightOperand).build();

        /* ASSERT */
        var value = result.getRightOperand();
        assertNotNull(value);
        assertEquals(rightOperand, value);
    }

    @Test
    void setRightOperandReference() {
        /* ACT */
        var result = new IdsConstraintBuilder().rightOperandReference(reference).build();

        /* ASSERT */
        var value = result.getRightOperandReference();
        assertNotNull(value);
        assertEquals(reference, value);
    }

    @Test
    void setUnit() {
        /* ACT */
        var result = new IdsConstraintBuilder().unit(unit).build();

        /* ASSERT */
        var value = result.getUnit();
        assertNotNull(value);
        assertEquals(unit, value);
    }

    @Test
    void setPipEndpoint() {
        /* ACT */
        var result = new IdsConstraintBuilder().pipEndpoint(endpoint).build();

        /* ASSERT */
        var value = result.getPipEndpoint();
        assertNotNull(value);
        assertEquals(endpoint, value);
    }

    @Test
    void build() {
        /* ACT */
        var result = (IdsConstraintImpl) new IdsConstraintBuilder(id)
                .leftOperand(leftOperand)
                .operator(operator)
                .rightOperand(rightOperand)
                .rightOperandReference(reference)
                .unit(unit)
                .pipEndpoint(endpoint)
                .build();

        /* ASSERT */
        assertNotNull(result);
        assertEquals(result.getId(), id);
        assertEquals(result.getLeftOperandAsString(), leftOperand);
        assertEquals(result.getOperator(), operator);
        assertEquals(result.getRightOperand(), rightOperand);
        assertEquals(result.getRightOperandReference(), reference);
        assertEquals(result.getUnit(), unit);
        assertEquals(result.getPipEndpoint(), endpoint);
    }

    @Test
    void serialize() throws JsonProcessingException {
        /* ARRANGE */
        var constraint = (IdsConstraintImpl) new IdsConstraintBuilder(id)
                .leftOperand(leftOperand)
                .operator(operator)
                .rightOperand(rightOperand)
                .rightOperandReference(reference)
                .unit(unit)
                .pipEndpoint(endpoint)
                .build();

        /* ACT */
        var result = new ObjectMapper().writeValueAsString(constraint);

        /* ASSERT */
        assertNotEquals("", result);
    }

    @Test
    void failedConstraintDeserialization() throws JsonProcessingException {
        /* ARRANGE */
        var constraint = (IdsConstraintImpl) new IdsConstraintBuilder(id)
                .leftOperand(leftOperand)
                .operator(operator)
                .rightOperand(rightOperand)
                .rightOperandReference(reference)
                .unit(unit)
                .pipEndpoint(endpoint)
                .build();
        var objectMapper = new ObjectMapper();
        var constraintAsString = objectMapper.writeValueAsString(constraint);

        /* ACT */
        var result = objectMapper.readValue(constraintAsString, Constraint.class);

        /* ASSERT */
        assertNotNull(result);
        assertTrue(result instanceof ConstraintImpl);
        assertNull(result.getLeftOperand());
    }

    @Test
    void deserializeConstraint() throws JsonProcessingException {
        /* ARRANGE */
        var constraint = (IdsConstraintImpl) new IdsConstraintBuilder(id)
                .leftOperand(leftOperand)
                .operator(operator)
                .rightOperand(rightOperand)
                .rightOperandReference(reference)
                .unit(unit)
                .pipEndpoint(endpoint)
                .build();

        var objectMapper = new ObjectMapper();
        objectMapper.registerSubtypes(IdsConstraintImpl.class);
        var constraintAsString = objectMapper.writeValueAsString(constraint);

        /* ACT */
        var result = objectMapper.readValue(constraintAsString, Constraint.class);

        /* ASSERT */
        assertNotNull(result);
        assertTrue(result instanceof IdsConstraintImpl);
        assertNull(result.getLeftOperand());
        assertEquals(id, result.getId());
        assertEquals(leftOperand, ((IdsConstraintImpl) result).getLeftOperandAsString());
        assertEquals(operator, result.getOperator());
        assertEquals(endpoint, result.getPipEndpoint());
        assertEquals(rightOperand.getType(), result.getRightOperand().getType());
        assertEquals(rightOperand.getValue(), result.getRightOperand().getValue());
        assertEquals(reference, result.getRightOperandReference());
        assertEquals(unit, result.getUnit());
    }

    @Test
    void deserializePermission() throws JsonProcessingException {
        /* ARRANGE */
        var constraint = (IdsConstraintImpl) new IdsConstraintBuilder(id)
                .leftOperand(leftOperand)
                .operator(operator)
                .rightOperand(rightOperand)
                .rightOperandReference(reference)
                .unit(unit)
                .pipEndpoint(endpoint)
                .build();
        var permission = new PermissionBuilder()
                ._action_(Action.USE)
                ._constraint_(constraint)
                ._assignee_(URI.create("assignee"))
                ._assigner_(URI.create("assigner"))
                ._target_(URI.create("target"))
                .build();

        var objectMapper = new ObjectMapper();
        objectMapper.registerSubtypes(IdsConstraintImpl.class);
        var permissionAsString = objectMapper.writeValueAsString(permission);

        /* ACT */
        var result = objectMapper.readValue(permissionAsString, Permission.class);

        /* ASSERT */
        assertNotNull(result);
        assertNotNull(result.getConstraint());

        var resultConstraint = (Constraint) result.getConstraint().get(0);
        assertNotNull(resultConstraint);
        assertTrue(resultConstraint instanceof IdsConstraintImpl);
        assertNull(resultConstraint.getLeftOperand());
        assertEquals(id, resultConstraint.getId());
        assertEquals(leftOperand, ((IdsConstraintImpl) resultConstraint).getLeftOperandAsString());
        assertEquals(operator, resultConstraint.getOperator());
        assertEquals(endpoint, resultConstraint.getPipEndpoint());
        assertEquals(rightOperand.getType(), resultConstraint.getRightOperand().getType());
        assertEquals(rightOperand.getValue(), resultConstraint.getRightOperand().getValue());
        assertEquals(reference, resultConstraint.getRightOperandReference());
        assertEquals(unit, resultConstraint.getUnit());
    }
}
