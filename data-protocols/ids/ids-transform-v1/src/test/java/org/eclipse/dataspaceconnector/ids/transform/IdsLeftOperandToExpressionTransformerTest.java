/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.LeftOperand;
import org.eclipse.dataspaceconnector.policy.model.Expression;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdsLeftOperandToExpressionTransformerTest {

    private final LeftOperand leftOperand = LeftOperand.PURPOSE;
    // subject
    private IdsLeftOperandToExpressionTransformer transformer;
    // mocks
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new IdsLeftOperandToExpressionTransformer();
        context = mock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(leftOperand, null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @ParameterizedTest
    @ArgumentsSource(TransformParameterArgumentSource.class)
    void transform(LeftOperand leftOperand, String expected) {
        Expression result = transformer.transform(leftOperand, context);

        assertThat(result).isNotNull().isInstanceOf(LiteralExpression.class);
        assertThat((LiteralExpression) result).extracting(LiteralExpression::getValue)
                .isEqualTo(expected);
    }

    static class TransformParameterArgumentSource implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.arguments(LeftOperand.ABSOLUTE_SPATIAL_POSITION, "ABSOLUTE_SPATIAL_POSITION"),
                    Arguments.arguments(LeftOperand.COUNT, "COUNT"),
                    Arguments.arguments(LeftOperand.DELAY, "DELAY"),
                    Arguments.arguments(LeftOperand.ELAPSED_TIME, "ELAPSED_TIME"),
                    Arguments.arguments(LeftOperand.ENDPOINT, "ENDPOINT"),
                    Arguments.arguments(LeftOperand.EVENT, "EVENT"),
                    Arguments.arguments(LeftOperand.PATH, "PATH"),
                    Arguments.arguments(LeftOperand.PAYMENT, "PAYMENT"),
                    Arguments.arguments(LeftOperand.PAY_AMOUNT, "PAY_AMOUNT"),
                    Arguments.arguments(LeftOperand.POLICY_EVALUATION_TIME, "POLICY_EVALUATION_TIME"),
                    Arguments.arguments(LeftOperand.PURPOSE, "PURPOSE"),
                    Arguments.arguments(LeftOperand.QUANTITY, "QUANTITY"),
                    Arguments.arguments(LeftOperand.RECURRENCE_RATE, "RECURRENCE_RATE"),
                    Arguments.arguments(LeftOperand.SECURITY_LEVEL, "SECURITY_LEVEL"),
                    Arguments.arguments(LeftOperand.STATE, "STATE"),
                    Arguments.arguments(LeftOperand.SYSTEM, "SYSTEM"),
                    Arguments.arguments(LeftOperand.USER, "USER")
            );
        }
    }
}