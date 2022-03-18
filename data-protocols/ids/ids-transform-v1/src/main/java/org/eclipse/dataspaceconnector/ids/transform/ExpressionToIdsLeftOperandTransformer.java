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

import de.fraunhofer.iais.eis.LeftOperand;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.policy.model.Expression;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ExpressionToIdsLeftOperandTransformer implements IdsTypeTransformer<Expression, LeftOperand> {

    @Override
    public Class<Expression> getInputType() {
        return Expression.class;
    }

    @Override
    public Class<LeftOperand> getOutputType() {
        return LeftOperand.class;
    }

    @Override
    public @Nullable LeftOperand transform(Expression object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        if (!(object instanceof LiteralExpression)) {
            context.reportProblem(String.format("Cannot transform %s. Supported constraints: '%s", object.getClass().getName(), LiteralExpression.class.getName()));
            return null;
        }

        var value = ((LiteralExpression) object).asString();

        LeftOperand leftOperand = null;

        // this is a hack until https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/338 is resolved - LeftOperand cannot be an enum
        if (value.contains("absoluteSpatialPosition")) {
            leftOperand = LeftOperand.ABSOLUTE_SPATIAL_POSITION;
        } else {
            try {
                leftOperand = LeftOperand.valueOf(value);
            } catch (IllegalArgumentException e) {
                context.reportProblem(String.format("Encountered undefined left operand type: %s. Error was: %s.", value, e.getMessage()));
            }

        }
        return leftOperand;
    }
}
