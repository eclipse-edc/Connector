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

package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.OrConstraint;
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
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_SCHEMA;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OR_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_XONE_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.transformer.to.TestInput.getExpanded;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToMultiplicityConstraintTransformerTest {

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToMultiplicityConstraintTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToMultiplicityConstraintTransformer();
    }

    @ParameterizedTest
    @ArgumentsSource(TransformArgumentsProvider.class)
    void transform_shouldReturnMultiplicityConstraint(String operandAttribute, Class<MultiplicityConstraint> expectedType) {
        var constraintJson = jsonFactory.createObjectBuilder().add(ODRL_OPERATOR_ATTRIBUTE, "EQ").build();
        var operand = jsonFactory.createObjectBuilder().add(operandAttribute, jsonFactory.createArrayBuilder().add(constraintJson).build()).build();
        var logicalConstraintJson = jsonFactory.createObjectBuilder()
                .add(ODRL_OPERAND_ATTRIBUTE, operand)
                .build();

        var atomicConstraint = AtomicConstraint.Builder.newInstance().build();
        when(context.transform(isA(JsonObject.class), eq(Constraint.class))).thenReturn(atomicConstraint);

        var result = transformer.transform(getExpanded(logicalConstraintJson), context);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(expectedType);
        assertThat(result.getConstraints())
                .isNotNull()
                .hasSize(1)
                .contains(atomicConstraint);
        verify(context, times(1)).transform(isA(JsonObject.class), eq(Constraint.class));
    }

    @ParameterizedTest
    @ArgumentsSource(TransformArgumentsProvider.class)
    void transform_shouldReturnEmptyMultiplicityConstraint_whenNoConstraints(String operandSubProperty, Class<MultiplicityConstraint> expectedType) {
        var operand = jsonFactory.createObjectBuilder().add(operandSubProperty, jsonFactory.createArrayBuilder().build()).build();
        var logicalConstraintJson = jsonFactory.createObjectBuilder()
                .add(ODRL_OPERAND_ATTRIBUTE, operand)
                .build();

        var result = transformer.transform(getExpanded(logicalConstraintJson), context);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(expectedType);
        assertThat(result.getConstraints()).isNotNull().isEmpty();
        verify(context, never()).transform(any(JsonObject.class), eq(Constraint.class));
    }

    @Test
    void transform_shouldReportProblem_whenUnknownOperandAttribute() {
        var operand = jsonFactory.createObjectBuilder()
                .add(ODRL_SCHEMA + "unknown", jsonFactory.createArrayBuilder().build())
                .build();
        var invalidJson = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, JsonObject.EMPTY_JSON_OBJECT)
                .add(ODRL_OPERAND_ATTRIBUTE, operand)
                .build();

        var result = transformer.transform(getExpanded(invalidJson), context);

        assertThat(result).isNull();
        verify(context, times(1)).reportProblem(anyString());
    }

    static class TransformArgumentsProvider implements ArgumentsProvider {
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
