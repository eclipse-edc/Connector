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

package org.eclipse.dataspaceconnector.azure.cosmos.dialect;

import com.azure.cosmos.models.SqlParameter;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.dataspaceconnector.cosmos.azure.CosmosDocument.sanitize;

class WhereClause implements Clause {
    public static final String EQUALS_OPERATOR = "=";
    public static final String IN_OPERATOR = "IN";
    private static final List<String> SUPPORTED_OPERATOR = List.of(EQUALS_OPERATOR, IN_OPERATOR);
    private final String objectPrefix;
    private final List<SqlParameter> parameters = new ArrayList<>();
    private String where = "";

    public WhereClause(List<Criterion> criteria, String objectPrefix) {
        this.objectPrefix = objectPrefix;
        if (criteria != null) {
            criteria.stream().distinct().forEach(this::criterion);
        }
    }

    WhereClause() {
        objectPrefix = null;
    }


    public String getWhere() {
        return where;
    }

    @Override
    public String asString() {
        return getWhere();
    }

    @Override
    public @NotNull List<SqlParameter> getParameters() {
        return parameters;
    }

    private void criterion(Criterion criterion) {
        if (!SUPPORTED_OPERATOR.contains(criterion.getOperator())) {
            throw new IllegalArgumentException("Cannot build SqlParameter for operator: " + criterion.getOperator());
        }
        //        criterion = operandEscapeFunction.apply(criterion);
        var operandLeft = sanitize(criterion.getOperandLeft().toString());
        var operandRight = criterion.getOperandRight().toString();
        var queryParam = createQueryParam(operandLeft, criterion.getOperator(), operandRight);
        String param = queryParam.isEmpty() ? operandRight : "@" + operandLeft;
        where += parameters.isEmpty() ? "WHERE" : " AND";
        parameters.addAll(queryParam);
        where += String.format(" %s.%s %s %s", objectPrefix, operandLeft, criterion.getOperator(), param);
    }

    private List<SqlParameter> createQueryParam(String opLeft, String operator, String opRight) {
        switch (operator) {
            case EQUALS_OPERATOR:
                return List.of(new SqlParameter("@" + opLeft, opRight));
            case IN_OPERATOR:
                return List.of();
            default: //not actually needed, but it's good style to have a default case
                throw new IllegalArgumentException("Cannot build SqlParameter for operator: " + operator);
        }
    }

}
