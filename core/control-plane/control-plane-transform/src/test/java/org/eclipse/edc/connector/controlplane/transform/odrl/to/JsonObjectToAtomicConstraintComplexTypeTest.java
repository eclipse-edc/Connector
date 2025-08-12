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
import jakarta.json.JsonArray;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.transform.TestInput;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToAtomicConstraintComplexTypeTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToConstraintTransformer transformer;
    private JsonObject left;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToConstraintTransformer();
        when(context.problem()).thenReturn(new ProblemBuilder(context));
        left = jsonFactory.createObjectBuilder().add(VALUE, "left").build();
    }

    /**
     * Verifies a Json-ld expanded array containing two strings as-is.
     */
    @Test
    void verify_array_of_strings() {
        var right = jsonFactory.createArrayBuilder().add("value1").add("value2").build();

        var constraint = createConstraint(right);

        var result = transformer.transform(constraint, context);

        assertThat(result).isNotNull().asInstanceOf(type(AtomicConstraint.class))
                .extracting(AtomicConstraint::getRightExpression)
                .asInstanceOf(type(LiteralExpression.class))
                .extracting(LiteralExpression::getValue)
                .asInstanceOf(type(JsonArray.class))
                .satisfies(array -> {
                    assertThat(array.size()).isEqualTo(2);
                    assertThat(array.get(0)).asInstanceOf(type(JsonObject.class)).extracting(v -> v.getJsonString(VALUE).getString()).isEqualTo("value1");
                    assertThat(array.get(1)).asInstanceOf(type(JsonObject.class)).extracting(v -> v.getJsonString(VALUE).getString()).isEqualTo("value2");
                });
    }

    /**
     * Verifies a Json-Ld array containing a single element of a complex type has the complex type extracted and returned.
     */
    @Test
    void verify_array_of_single_complex_type() {
        var right = jsonFactory.createArrayBuilder().add(jsonFactory.createObjectBuilder(Map.of("edc:foo", "bar"))).build();

        var constraint = createConstraint(right);

        var result = transformer.transform(constraint, context);

        assertThat(result).isNotNull().asInstanceOf(type(AtomicConstraint.class))
                .extracting(AtomicConstraint::getRightExpression)
                .asInstanceOf(type(LiteralExpression.class))
                .extracting(LiteralExpression::getValue)
                .asInstanceOf(type(JsonObject.class))
                .extracting(o -> o.getJsonArray(EDC_NAMESPACE + "foo"))
                .satisfies(array -> {
                    assertThat(array.size()).isEqualTo(1);
                    assertThat(array.get(0)).asInstanceOf(type(JsonObject.class)).extracting(v -> v.getJsonString(VALUE).getString()).isEqualTo("bar");
                });
    }

    /**
     * Verifies a Json-Ld object with no {@code value} property is returned as-is.
     */
    @Test
    void verify_object_no_value() {
        var right = jsonFactory.createObjectBuilder(Map.of("edc:foo", "bar")).build();

        var constraint = createConstraint(right);

        var result = transformer.transform(constraint, context);

        assertThat(result).isNotNull().asInstanceOf(type(AtomicConstraint.class))
                .extracting(AtomicConstraint::getRightExpression)
                .asInstanceOf(type(LiteralExpression.class))
                .extracting(LiteralExpression::getValue)
                .asInstanceOf(type(Map.class))
                .extracting(m -> m.get(EDC_NAMESPACE + "foo"))
                .asInstanceOf(type(JsonArray.class))
                .extracting(a -> a.get(0))
                .asInstanceOf(type(JsonObject.class))
                .extracting(VALUE).asInstanceOf(type(JsonString.class))
                .extracting(JsonString::getString)
                .isEqualTo("bar");
    }

    /**
     * Verifies a Json-Ld object with a {@code value} property has the property extracted and transformed.
     */
    @Test
    void verify_object_with_value() {
        var right = jsonFactory.createObjectBuilder(Map.of(VALUE, "bar")).build();

        var constraint = createConstraint(right);

        var result = transformer.transform(constraint, context);

        assertThat(result).isNotNull().asInstanceOf(type(AtomicConstraint.class))
                .extracting(AtomicConstraint::getRightExpression)
                .asInstanceOf(type(LiteralExpression.class))
                .extracting(LiteralExpression::getValue)
                .isEqualTo("bar");
    }

    /**
     * Verifies string values are returned correctly.
     */
    @Test
    void verify_expanded_string() {
        var constraint = createConstraint("bar");

        var result = transformer.transform(constraint, context);

        assertThat(result).isNotNull().asInstanceOf(type(AtomicConstraint.class))
                .extracting(AtomicConstraint::getRightExpression)
                .asInstanceOf(type(LiteralExpression.class))
                .extracting(LiteralExpression::getValue)
                .isEqualTo("bar");
    }

    /**
     * Verifies int values are returned correctly.
     */
    @Test
    void verify_expanded_int() {
        var constraint = createConstraint(1);

        var result = transformer.transform(constraint, context);

        assertThat(result).isNotNull().asInstanceOf(type(AtomicConstraint.class))
                .extracting(AtomicConstraint::getRightExpression)
                .asInstanceOf(type(LiteralExpression.class))
                .extracting(LiteralExpression::getValue)
                .isEqualTo(1);
    }

    /**
     * Verifies decimal values are returned correctly.
     */
    @Test
    void verify_expanded_decimal() {
        var constraint = createConstraint(1.1d);

        var result = transformer.transform(constraint, context);

        assertThat(result).isNotNull().asInstanceOf(type(AtomicConstraint.class))
                .extracting(AtomicConstraint::getRightExpression)
                .asInstanceOf(type(LiteralExpression.class))
                .extracting(expr -> (BigDecimal) expr.getValue())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(BigDecimal.valueOf(1.1d));
    }

    private JsonObject createConstraint(Object value) {
        var constraint = jsonFactory.createObjectBuilder()
                .add(CONTEXT, jsonFactory.createObjectBuilder(Map.of(EDC_PREFIX, EDC_NAMESPACE)))
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, left)
                .add(ODRL_OPERATOR_ATTRIBUTE, EQ.toString());

        if (value instanceof String right) {
            constraint.add(ODRL_RIGHT_OPERAND_ATTRIBUTE, right);
        } else if (value instanceof Integer right) {
            constraint.add(ODRL_RIGHT_OPERAND_ATTRIBUTE, right);
        } else if (value instanceof Double right) {
            constraint.add(ODRL_RIGHT_OPERAND_ATTRIBUTE, right);
        } else if (value instanceof JsonValue right) {
            constraint.add(ODRL_RIGHT_OPERAND_ATTRIBUTE, right);
        }

        return TestInput.getExpanded(constraint.build());
    }

}
