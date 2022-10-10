/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.transformer;

import org.eclipse.dataspaceconnector.api.model.CriterionDto;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CriterionDtoToCriterionTransformerTest {

    private final CriterionDtoToCriterionTransformer transformer = new CriterionDtoToCriterionTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var dto = CriterionDto.Builder.newInstance()
                .operandLeft("left")
                .operator("=")
                .operandRight("right")
                .build();

        var criterion = transformer.transform(dto, context);

        assertThat(criterion).usingRecursiveComparison().isEqualTo(dto);
    }

    @Test
    void transform_nullInput() {
        var context = mock(TransformerContext.class);

        var definition = transformer.transform(null, context);

        assertThat(definition).isNull();
        verify(context).reportProblem("input criterion is null");
    }
}