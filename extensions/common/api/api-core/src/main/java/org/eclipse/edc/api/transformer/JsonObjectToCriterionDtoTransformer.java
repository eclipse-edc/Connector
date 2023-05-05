/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.api.transformer;

import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERAND_RIGHT;
import static org.eclipse.edc.api.model.CriterionDto.CRITERION_OPERATOR;

public class JsonObjectToCriterionDtoTransformer extends AbstractJsonLdTransformer<JsonObject, CriterionDto> {

    public JsonObjectToCriterionDtoTransformer() {
        super(JsonObject.class, CriterionDto.class);
    }

    @Override
    public @Nullable CriterionDto transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = CriterionDto.Builder.newInstance();

        visitProperties(object, k -> {
            switch (k) {
                case CRITERION_OPERAND_LEFT:
                    return v -> builder.operandLeft(transformString(v, context));
                case CRITERION_OPERAND_RIGHT:
                    return v -> builder.operandRight(transformString(v, context));
                case CRITERION_OPERATOR:
                    return v -> builder.operator(transformString(v, context));
                default:
                    return doNothing();
            }
        });

        return builder.build();
    }

}
