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
    private final String objectPrefix;
    private final List<SqlParameter> parameters = new ArrayList<>();
    private String where = "";

    WhereClause(List<Criterion> criteria, String objectPrefix) {
        this.objectPrefix = objectPrefix;
        if (criteria != null) {
            criteria.stream().distinct().forEach(this::parse);
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

    private void parse(Criterion criterion) {
        var expr = new CosmosConditionExpression(criterion, objectPrefix);
        var exprResult = expr.isValidExpression();
        if (exprResult.failed()) {
            throw new IllegalArgumentException("Cannot build WHERE clause, reason: " + String.join(", ", exprResult.getFailureMessages()));
        }

        where += where.startsWith("WHERE") ? " AND" : "WHERE"; //if we have a chained WHERE ... AND ... statement
        parameters.addAll(expr.getParameters());
        where += expr.toExpressionString();
    }

}
