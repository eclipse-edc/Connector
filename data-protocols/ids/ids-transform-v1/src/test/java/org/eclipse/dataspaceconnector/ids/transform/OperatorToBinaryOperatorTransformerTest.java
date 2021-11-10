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
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorToBinaryOperatorTransformerTest {

    // subject
    private OperatorToBinaryOperatorTransformer operatorToBinaryOperatorTransformer;

    // mocks
    private TransformerContext transformerContext;

    @BeforeEach
    void setUp() {
        operatorToBinaryOperatorTransformer = new OperatorToBinaryOperatorTransformer();

        transformerContext = EasyMock.mock(TransformerContext.class);
    }

    @ParameterizedTest
    @ArgumentsSource(TransformParameterArgumentSource.class)
    void transform(Operator source, BinaryOperator expected) {
        if (expected == null) {
            transformerContext.reportProblem(EasyMock.anyString());
        }

        EasyMock.replay(transformerContext);

        var result = operatorToBinaryOperatorTransformer.transform(source, transformerContext);

        assertThat(result).isEqualTo(expected);
    }

    static class TransformParameterArgumentSource implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.arguments(Operator.EQ, BinaryOperator.EQUALS),
                    Arguments.arguments(Operator.GT, BinaryOperator.GT),
                    Arguments.arguments(Operator.GEQ, BinaryOperator.GTEQ),
                    Arguments.arguments(Operator.LT, BinaryOperator.LT),
                    Arguments.arguments(Operator.LEQ, BinaryOperator.LTEQ),
                    Arguments.arguments(Operator.IN, BinaryOperator.IN),
                    Arguments.arguments(Operator.NEQ, null));
        }
    }

    @AfterEach
    void tearDown() {
        EasyMock.verify(transformerContext);
    }
}