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

package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_RIGHT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERATOR;

public class JsonObjectToCriterionTransformer extends AbstractJsonLdTransformer<JsonObject, Criterion> {

    public JsonObjectToCriterionTransformer() {
        super(JsonObject.class, Criterion.class);
    }

    @Override
    public @Nullable Criterion transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var builder = Criterion.Builder.newInstance();

        visitProperties(input, key -> {
            switch (key) {
                case CRITERION_OPERAND_LEFT:
                    return v -> builder.operandLeft(transformGenericProperty(v, context));
                case CRITERION_OPERAND_RIGHT:
                    return v -> builder.operandRight(transformGenericProperty(v, context));
                case CRITERION_OPERATOR:
                    return v -> builder.operator(transformString(v, context));
                default:
                    return doNothing();
            }
        });

        return builder.build();
    }

}
