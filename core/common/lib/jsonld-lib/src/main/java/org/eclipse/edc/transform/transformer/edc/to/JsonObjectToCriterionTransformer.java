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

package org.eclipse.edc.transform.transformer.edc.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_RIGHT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERATOR;

public class JsonObjectToCriterionTransformer extends AbstractJsonLdTransformer<JsonObject, Criterion> {

    private final CriterionOperatorRegistry criterionOperatorRegistry;

    public JsonObjectToCriterionTransformer(CriterionOperatorRegistry criterionOperatorRegistry) {
        super(JsonObject.class, Criterion.class);
        this.criterionOperatorRegistry = criterionOperatorRegistry;
    }

    @Override
    public @Nullable Criterion transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = Criterion.Builder.newInstance();

        builder.operandLeft(transformString(object.get(CRITERION_OPERAND_LEFT), context));

        var operator = transformString(object.get(CRITERION_OPERATOR), context);
        builder.operator(operator);

        var operandRight = object.get(CRITERION_OPERAND_RIGHT);
        if (Iterable.class.isAssignableFrom(criterionOperatorRegistry.get(operator).rightOperandClass())) {
            builder.operandRight(operandRight.asJsonArray().stream().map(this::nodeValue).toList());
        } else {
            builder.operandRight(transformGenericProperty(operandRight, context));
        }

        return builder.build();
    }

}
