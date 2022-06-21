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

import de.fraunhofer.iais.eis.BinaryOperator;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

class IdsBinaryOperatorToOperatorTransformerTest {

    // subject
    private IdsBinaryOperatorToOperatorTransformer transformer;

    // mocks
    private TransformerContext transformerContext;

    @BeforeEach
    void setUp() {
        transformer = new IdsBinaryOperatorToOperatorTransformer();

        transformerContext = mock(TransformerContext.class);
    }

    @ParameterizedTest
    @ArgumentsSource(IdsBinaryOperatorToOperatorTransformerTest.TransformParameterArgumentSource.class)
    void transform(BinaryOperator source, Operator expected) {
        if (expected == null) {
            transformerContext.reportProblem(anyString());
        }

        var result = transformer.transform(source, transformerContext);

        assertThat(result).isEqualTo(expected);
    }

    static class TransformParameterArgumentSource implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.arguments(BinaryOperator.EQUALS, Operator.EQ),
                    Arguments.arguments(BinaryOperator.EQ, Operator.EQ),
                    Arguments.arguments(BinaryOperator.GT, Operator.GT),
                    Arguments.arguments(BinaryOperator.GTEQ, Operator.GEQ),
                    Arguments.arguments(BinaryOperator.LT, Operator.LT),
                    Arguments.arguments(BinaryOperator.LTEQ, Operator.LEQ),
                    Arguments.arguments(BinaryOperator.IN, Operator.IN));
        }
    }
}