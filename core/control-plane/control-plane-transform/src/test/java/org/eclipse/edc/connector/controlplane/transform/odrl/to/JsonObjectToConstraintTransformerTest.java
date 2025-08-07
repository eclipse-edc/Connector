/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.odrl.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.controlplane.transform.TestInput;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.eclipse.edc.transform.spi.ProblemBuilder;
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
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OR_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_XONE_CONSTRAINT_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToConstraintTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToConstraintTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToConstraintTransformer();
        when(context.problem()).thenReturn(new ProblemBuilder(context));
    }

    @Test
    void atomicConstraint_attributesAsArrays_returnConstraint() {
        var left = "left";
        var right = "right";
        when(context.transform(any(), eq(Operator.class))).thenReturn(Operator.GEQ);
        var constraint = jsonFactory.createObjectBuilder()
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, jsonFactory.createArrayBuilder().add(id(left)))
                .add(ODRL_OPERATOR_ATTRIBUTE, jsonFactory.createArrayBuilder().add(id("gteq")))
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, jsonFactory.createArrayBuilder().add(value(right)))
                .build();

        var result = transformer.transform(TestInput.getExpanded(constraint), context);

        assertThat(result).isNotNull().asInstanceOf(type(AtomicConstraint.class)).satisfies(atomicConstraint -> {
            assertThat(((LiteralExpression) atomicConstraint.getLeftExpression()).getValue()).isEqualTo(left);
            assertThat(atomicConstraint.getOperator()).isEqualTo(Operator.GEQ);
            assertThat(((LiteralExpression) atomicConstraint.getRightExpression()).getValue()).isEqualTo(right);
        });
        verify(context).transform(id("gteq").build(), Operator.class);
    }

    @Test
    void atomicConstraint_invalidAttributes_reportProblems() {
        var jsonNumber = Json.createValue(1);

        var constraint = jsonFactory.createObjectBuilder()
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, jsonNumber)
                .add(ODRL_OPERATOR_ATTRIBUTE, jsonNumber)
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, jsonNumber)
                .build();

        var result = transformer.transform(TestInput.getExpanded(constraint), context);

        assertThat(result).isNull();
        verify(context, atLeast(1)).reportProblem(anyString());
    }

    @Test
    void atomicConstraint_missingMandatoryAttributes_reportProblems() {
        var constraint = jsonFactory.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .build();

        transformer.transform(TestInput.getExpanded(constraint), context);

        verify(context, atLeast(1)).reportProblem(anyString());
    }

    @ParameterizedTest
    @ArgumentsSource(OperandsProvider.class)
    void logicalConstraint_shouldReturnMultiplicityConstraint(String operand, Class<MultiplicityConstraint> expectedType) {
        var constraintJson = jsonFactory.createObjectBuilder()
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, "left")
                .add(ODRL_OPERATOR_ATTRIBUTE, "EQ")
                .build();
        var logicalConstraintJson = jsonFactory.createObjectBuilder()
                .add(operand, jsonFactory.createArrayBuilder()
                        .add(constraintJson)
                        .build())
                .build();

        var atomicConstraint = AtomicConstraint.Builder.newInstance().build();
        when(context.transform(any(), eq(Constraint.class))).thenReturn(atomicConstraint);

        var result = transformer.transform(TestInput.getExpanded(logicalConstraintJson), context);

        assertThat(result).isNotNull().isInstanceOf(expectedType)
                .asInstanceOf(type(MultiplicityConstraint.class))
                .extracting(MultiplicityConstraint::getConstraints)
                .isNotNull().asList().hasSize(1).contains(atomicConstraint);
        verify(context, times(1)).transform(isA(JsonObject.class), eq(Constraint.class));
    }

    @ParameterizedTest
    @ArgumentsSource(OperandsProvider.class)
    void logicalConstraint_shouldReturnEmptyMultiplicityConstraint_whenNoConstraints(String operand,
                                                                                     Class<MultiplicityConstraint> expectedType) {
        var logicalConstraintJson = jsonFactory.createObjectBuilder()
                .add(operand, jsonFactory.createArrayBuilder().build())
                .build();

        var result = transformer.transform(TestInput.getExpanded(logicalConstraintJson), context);

        assertThat(result).isNotNull().isInstanceOf(expectedType)
                .asInstanceOf(type(MultiplicityConstraint.class))
                .extracting(MultiplicityConstraint::getConstraints)
                .isNotNull().asList().isEmpty();
        verify(context, never()).transform(any(JsonObject.class), eq(Constraint.class));
    }

    private JsonObjectBuilder id(String value) {
        return jsonFactory.createObjectBuilder().add(ID, value);
    }

    private JsonObjectBuilder value(String value) {
        return jsonFactory.createObjectBuilder().add(VALUE, value);
    }

    static class OperandsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(ODRL_AND_CONSTRAINT_ATTRIBUTE, AndConstraint.class),
                    Arguments.of(ODRL_OR_CONSTRAINT_ATTRIBUTE, OrConstraint.class),
                    Arguments.of(ODRL_XONE_CONSTRAINT_ATTRIBUTE, XoneConstraint.class)
            );
        }
    }


}
