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
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.transformer.to.TestInput.getExpanded;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToAtomicConstraintTransformerTest {
    private static final String LEFT_VALUE = "left";
    private static final String RIGHT_VALUE = "right";

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToAtomicConstraintTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToAtomicConstraintTransformer();
    }

    @ParameterizedTest
    @MethodSource("jsonSource")
    void transform_operatorAsString_returnConstraint(JsonObject constraint) {

        when(context.hasProblems()).thenReturn(false);

        var result = (AtomicConstraint) transformer.transform(getExpanded(constraint), context);

        assertThat(result).isNotNull();
        assertThat(result.getLeftExpression())
                .asInstanceOf(type(LiteralExpression.class))
                .satisfies(le -> assertThat(le.getValue()).isEqualTo(LEFT_VALUE));
        assertThat(result.getOperator()).isEqualTo(EQ);
        assertThat(result.getRightExpression())
                .asInstanceOf(type(LiteralExpression.class))
                .satisfies(re -> assertThat(re.getValue()).isEqualTo(RIGHT_VALUE));

        verify(context).hasProblems();
    }

    @Test
    void transform_invalidAttributes_reportProblems() {
        var jsonNumber = Json.createValue(1);

        var constraint = jsonFactory.createObjectBuilder()
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, jsonNumber)
                .add(ODRL_OPERATOR_ATTRIBUTE, jsonNumber)
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, jsonNumber)
                .build();

        transformer.transform(getExpanded(constraint), context);

        verify(context, times(3)).reportProblem(anyString());
    }

    static Stream<JsonObject> jsonSource() {
        var jsonFactory = Json.createBuilderFactory(Map.of());
        return Stream.of(
                // as object
                jsonFactory.createObjectBuilder()
                        .add(ODRL_LEFT_OPERAND_ATTRIBUTE, jsonFactory.createObjectBuilder().add(VALUE, LEFT_VALUE))
                        .add(ODRL_OPERATOR_ATTRIBUTE, EQ.name())
                        .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, jsonFactory.createObjectBuilder().add(VALUE, RIGHT_VALUE))
                        .build(),

                // as arrays
                jsonFactory.createObjectBuilder()
                        .add(ODRL_LEFT_OPERAND_ATTRIBUTE, jsonFactory.createArrayBuilder().add(jsonFactory.createObjectBuilder().add(VALUE, LEFT_VALUE)))
                        .add(ODRL_OPERATOR_ATTRIBUTE, jsonFactory.createArrayBuilder().add(jsonFactory.createObjectBuilder().add(VALUE, EQ.name())))
                        .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, jsonFactory.createArrayBuilder().add(jsonFactory.createObjectBuilder().add(VALUE, RIGHT_VALUE)))
                        .build()
        );
    }


}
