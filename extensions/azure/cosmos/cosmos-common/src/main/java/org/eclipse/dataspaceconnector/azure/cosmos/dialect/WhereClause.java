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

/**
 * Represents in a structural way a WHERE clause in an SQL statement. Attempts to use parameterized statements using
 * {@link SqlParameter} if possible. Currently, this is only implemented for the equals-operator ("=").
 * <p>
 * For every {@link Criterion} that is passed in, another {@code WHERE}- or {@code AND}-clause is appended.
 *
 * <p>
 * Optionally an {@code orderPrefix} can be specified, which represents the "path" of the property. This is particularly
 * relevant for CosmosDB queries, e.g:
 * <pre>
 *     SELECT * FROM YourDocument WHERE YourDocument.Header.Author = 'Foo Bar'
 * </pre>
 * In this case the {@code orderPrefix} would have to be {@code "YourDocument.Header"}.
 */
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

    @Override
    public String asString() {
        return getWhere();
    }

    @Override
    public @NotNull List<SqlParameter> getParameters() {
        return parameters;
    }

    String getWhere() {
        return where;
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
