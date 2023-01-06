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
 *       Fraunhofer Institute for Software and Systems Engineering - Initial API and Implementation
 *
 */

package org.eclipse.edc.protocol.ids.transform.type.policy;

import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExpressionToIdsLeftOperandTransformer implements IdsTypeTransformer<Expression, String> {

    @Override
    public Class<Expression> getInputType() {
        return Expression.class;
    }

    @Override
    public Class<String> getOutputType() {
        return String.class;
    }

    @Override
    public @Nullable String transform(@NotNull Expression object, @NotNull TransformerContext context) {
        if (!(object instanceof LiteralExpression)) {
            context.reportProblem(String.format("Cannot transform %s. Supported type: LiteralExpression", object.getClass().getName()));
            return null;
        }

        return ((LiteralExpression) object).asString();
    }
}
