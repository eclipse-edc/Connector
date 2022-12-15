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
 *       Fraunhofer Insitute for Software and Systems Engineering
 *
 */

package org.eclipse.edc.protocol.ids.transform.type.policy;

import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExpressionFromIdsLeftOperandTransformer implements IdsTypeTransformer<String, Expression> {

    @Override
    public Class<String> getInputType() {
        return String.class;
    }

    @Override
    public Class<Expression> getOutputType() {
        return Expression.class;
    }

    @Override
    public @Nullable Expression transform(@NotNull String object, @NotNull TransformerContext context) {
        return new LiteralExpression(object);
    }
}
