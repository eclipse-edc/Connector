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

package org.eclipse.dataspaceconnector.ids.transform;

import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.policy.model.Expression;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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
    public @Nullable String transform(Expression object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        if (!(object instanceof LiteralExpression)) {
            context.reportProblem(String.format("Cannot transform %s. Supported constraints: '%s", object.getClass().getName(), LiteralExpression.class.getName()));
            return null;
        }

        return ((LiteralExpression) object).asString();
    }
}
