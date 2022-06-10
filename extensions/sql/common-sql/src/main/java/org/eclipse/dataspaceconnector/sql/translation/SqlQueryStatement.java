/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.sql.translation;

import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Maps a {@link QuerySpec} to a single SQL {@code SELECT ... FROM ... WHERE ...} statement. The {@code SELECT ...} part
 * is passed in through the constructor, and the rest of the query is assembled dynamically, based on the
 * {@link QuerySpec} and the {@link TranslationMapping}.
 */
public class SqlQueryStatement {

    private static final String LIMIT = "LIMIT ? ";
    private static final String OFFSET = "OFFSET ?";
    private static final String WHERE_TOKEN = "WHERE";
    private static final String AND_TOKEN = "AND";
    private final String selectStatement;
    private final List<String> whereClauses = new ArrayList<>();
    private final List<Object> parameters = new ArrayList<>();

    /**
     * Initializes this SQL Query Statement with a SELECT clause, a {@link QuerySpec} and a translation mapping.
     *
     * @param selectStatement The SELECT clause, e.g. {@code SELECT * FROM your_table}
     * @param query a {@link QuerySpec} that contains a query in the canonical format
     * @param rootModel A {@link TranslationMapping} that enables mapping from canonical to the SQL-specific
     *         model/format
     */
    public SqlQueryStatement(String selectStatement, QuerySpec query, TranslationMapping rootModel) {
        this(selectStatement);
        initialize(query, rootModel);
    }

    /**
     * Initializes this SQL Query Statement with a SELECT clause
     *
     * @param selectStatement The SELECT clause, e.g. {@code SELECT * FROM your_table}
     */
    public SqlQueryStatement(String selectStatement) {
        this.selectStatement = selectStatement;
    }

    /**
     * Represents this query as SQL string, including parameter placeholders (?)
     *
     * @return the query as SQL statement
     */
    public String getQueryAsString() {
        return selectStatement + " " +
                String.join(" ", whereClauses) + " " +
                LIMIT +
                OFFSET +
                ";";
    }

    /**
     * SQL supports parameter substitution (in prepared statements), this method returns a list of those placeholders.
     *
     * @return an array of parameters that can be used for prepared statements
     */
    public Object[] getParameters() {
        return parameters.toArray(Object[]::new);
    }

    public void addParameter(Object param) {
        parameters.add(param);
    }

    private void initialize(QuerySpec query, TranslationMapping rootModel) {
        whereClauses.clear();
        parameters.clear();

        var expr = query.getFilterExpression();
        expr.forEach(e -> parseExpression(e, rootModel));

        parameters.add(query.getLimit());
        parameters.add(query.getOffset());
    }

    /**
     * Parses a single {@link Criterion} into a {@code WHERE} or an {@code AND} clause, and puts them onto the statement
     * stack.
     *
     * @param criterion One single query clause
     * @param rootModel The root mapping model for the query
     */
    private void parseExpression(Criterion criterion, TranslationMapping rootModel) {
        var columnName = rootModel.getStatement(criterion.getOperandLeft().toString());

        if (columnName == null) {
            throw new IllegalArgumentException(format("Operand \"%s\" cannot be mapped to SQL Schema", criterion.getOperandLeft()));
        }
        var newCriterion = new Criterion(columnName, criterion.getOperator(), criterion.getOperandRight());

        var prefix = whereClauses.isEmpty() ? WHERE_TOKEN : AND_TOKEN;
        var conditionExpr = new SqlConditionExpression(newCriterion);

        if (conditionExpr.isValidExpression().failed()) {
            throw new IllegalArgumentException("This expression is not valid!");
        }

        var clause = format("%s %s %s %s", prefix, columnName, newCriterion.getOperator(), conditionExpr.toValuePlaceholder());
        whereClauses.add(clause);
        var params = conditionExpr.toStatementParameter().skip(1).collect(Collectors.toList());
        parameters.addAll(params);
    }
}
