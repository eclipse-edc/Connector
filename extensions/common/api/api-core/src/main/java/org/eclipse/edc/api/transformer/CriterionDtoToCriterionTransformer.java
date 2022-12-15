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

public class CriterionDtoToCriterionTransformer implements DtoTransformer<CriterionDto, Criterion> {

    @Override
    public Class<CriterionDto> getInputType() {
        return CriterionDto.class;
    }

    @Override
    public Class<Criterion> getOutputType() {
        return Criterion.class;
    }

    @Override
    public @Nullable Criterion transform(@NotNull CriterionDto object, @NotNull TransformerContext context) {
        return new Criterion(object.getOperandLeft(), object.getOperator(), object.getOperandRight());
    }
}
