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

package org.eclipse.edc.api.transformer;

import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

}
