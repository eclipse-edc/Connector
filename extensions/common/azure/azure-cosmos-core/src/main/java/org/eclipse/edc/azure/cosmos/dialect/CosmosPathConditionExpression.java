/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.azure.cosmos.dialect;

import org.eclipse.edc.azure.cosmos.CosmosDocument;
import org.eclipse.edc.spi.query.Criterion;

class CosmosPathConditionExpression extends ConditionExpression {

    private final String objectPrefix;

    CosmosPathConditionExpression(Criterion criterion) {
        this(criterion, null);
    }

    CosmosPathConditionExpression(Criterion criterion, String objectPrefix) {
        super(criterion);
        this.objectPrefix = objectPrefix;
    }


    @Override
    public String getFieldPath() {
        return getCriterion().getOperandLeft().toString();
    }

    /**
     * Converts the {@link Criterion} into a string representation, that uses statement placeholders ("@xyz"). The
     * corresponding parameters are available using {@link CosmosPathConditionExpression#getParameters()}.
     */
    @Override
    public String toExpressionString() {
        var operandLeft = CosmosDocument.sanitize(getCriterion().getOperandLeft().toString());
        return objectPrefix != null ?
                String.format(" %s.%s %s %s", objectPrefix, operandLeft, getCriterion().getOperator(), toValuePlaceholder()) :
                String.format(" %s %s %s", operandLeft, getCriterion().getOperator(), toValuePlaceholder());
    }


}
