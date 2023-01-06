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
 *       Fraunhofer Insitute for Software and Systems Engineering
 *
 */

package org.eclipse.edc.protocol.ids.transform;

import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.protocol.ids.transform.type.policy.ExpressionFromIdsLeftOperandTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ExpressionFromIdsLeftOperandTransformerTest {

    private final String leftOperand = "PURPOSE";

    // subject
    private ExpressionFromIdsLeftOperandTransformer transformer;
    // mocks
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new ExpressionFromIdsLeftOperandTransformer();
        context = mock(TransformerContext.class);
    }

    @ParameterizedTest
    @ArgumentsSource(TransformParameterArgumentSource.class)
    void transform(String leftOperand, String expected) {
        Expression result = transformer.transform(leftOperand, context);

        assertThat(result).isNotNull().isInstanceOf(LiteralExpression.class);
        assertThat((LiteralExpression) result).extracting(LiteralExpression::getValue)
                .isEqualTo(expected);
    }

    static class TransformParameterArgumentSource implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.arguments("ABSOLUTE_SPATIAL_POSITION", "ABSOLUTE_SPATIAL_POSITION"),
                    Arguments.arguments("COUNT", "COUNT"),
                    Arguments.arguments("DELAY", "DELAY"),
                    Arguments.arguments("ELAPSED_TIME", "ELAPSED_TIME"),
                    Arguments.arguments("ENDPOINT", "ENDPOINT"),
                    Arguments.arguments("EVENT", "EVENT"),
                    Arguments.arguments("PATH", "PATH"),
                    Arguments.arguments("PAYMENT", "PAYMENT"),
                    Arguments.arguments("PAY_AMOUNT", "PAY_AMOUNT"),
                    Arguments.arguments("POLICY_EVALUATION_TIME", "POLICY_EVALUATION_TIME"),
                    Arguments.arguments("PURPOSE", "PURPOSE"),
                    Arguments.arguments("QUANTITY", "QUANTITY"),
                    Arguments.arguments("RECURRENCE_RATE", "RECURRENCE_RATE"),
                    Arguments.arguments("SECURITY_LEVEL", "SECURITY_LEVEL"),
                    Arguments.arguments("STATE", "STATE"),
                    Arguments.arguments("SYSTEM", "SYSTEM"),
                    Arguments.arguments("USER", "USER")
            );
        }
    }
}
