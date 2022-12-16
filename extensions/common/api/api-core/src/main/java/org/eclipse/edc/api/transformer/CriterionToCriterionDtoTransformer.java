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
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CriterionToCriterionDtoTransformer implements DtoTransformer<Criterion, CriterionDto> {

    @Override
    public Class<Criterion> getInputType() {
        return Criterion.class;
    }

    @Override
    public Class<CriterionDto> getOutputType() {
        return CriterionDto.class;
    }

    @Override
    public @Nullable CriterionDto transform(@NotNull Criterion object, @NotNull TransformerContext context) {
        return CriterionDto.Builder.newInstance()
                .operandLeft(object.getOperandLeft())
                .operator(object.getOperator())
                .operandRight(object.getOperandRight())
                .build();
    }
}
