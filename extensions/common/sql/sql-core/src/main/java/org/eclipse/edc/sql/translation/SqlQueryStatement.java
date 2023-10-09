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

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

/**
 * Maps a {@link QuerySpec} to a single SQL {@code SELECT ... FROM ... WHERE ...} statement. The {@code SELECT ...} part
 * is passed in through the constructor, and the rest of the query is assembled dynamically, based on the
 * {@link QuerySpec} and the {@link TranslationMapping}.
 */
public class SqlQueryStatement {

    private static final String LIMIT = "LIMIT ? ";
    private static final String OFFSET = "OFFSET ?";

    private static final String ORDER_BY_TOKEN = "ORDER BY %s %s";

    private final String selectStatement;
    private final List<String> whereClauses = new ArrayList<>();
    private final List<Object> parameters = new ArrayList<>();

    private String orderByClause = "";
    private final int limit;
    private final int offset;

    /**
     * Initializes this SQL Query Statement with a SELECT clause, a {@link QuerySpec} and a translation mapping.
     *
     * @param selectStatement The SELECT clause, e.g. {@code SELECT * FROM your_table}
     * @param query           a {@link QuerySpec} that contains a query in the canonical format
     * @param rootModel       A {@link TranslationMapping} that enables mapping from canonical to the SQL-specific
     *                        model/format
     */
    public SqlQueryStatement(String selectStatement, QuerySpec query, TranslationMapping rootModel) {
        this(selectStatement, query.getLimit(), query.getOffset());
        initialize(query, rootModel);
    }

    /**
     * Initializes this SQL Query Statement with a SELECT clause, LIMIT and OFFSET values.
     *
     * @param selectStatement The SELECT clause, e.g. {@code SELECT * FROM your_table}
     * @param limit the limit value.
     * @param offset the offset value.
     */
    public SqlQueryStatement(String selectStatement, int limit, int offset) {
        this.selectStatement = selectStatement;
        this.limit = limit;
        this.offset = offset;
    }

    /**
     * Represents this query as SQL string, including parameter placeholders (?)
     *
     * @return the query as SQL statement
     */
    public String getQueryAsString() {
        var whereClause = whereClauses.isEmpty() ? "" : whereClauses.stream().collect(joining(" AND ", "WHERE ", " "));

        return selectStatement + " " +
                whereClause +
                orderByClause +
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
        var params = new ArrayList<>(parameters);
        params.add(limit);
        params.add(offset);
        return params.toArray(Object[]::new);
    }

    /**
     * Add where clause with related parameters. If it contains multiple clauses better wrap it with parenthesis
     *
     * @param clause the SQL where clause.
     * @param parameters the parameters.
     * @return self.
     */
    public SqlQueryStatement addWhereClause(String clause, Object... parameters) {
        whereClauses.add(clause);
        Collections.addAll(this.parameters, parameters);
        return this;
    }

    /**
     * Add where clause. If it contains multiple clauses better wrap it with parenthesis
     *
     * @param clause the SQL where clause.
     * @deprecated please use {@link #addWhereClause(String, Object...)}
     */
    @Deprecated(since = "0.3.1")
    public void addWhereClause(String clause) {
        whereClauses.add(clause);
    }

    /**
     * Add parameter.
     *
     * @param parameter the parameter.
     * @deprecated please use {@link #addWhereClause(String, Object...)}
     */
    @Deprecated(since = "0.3.1")
    public void addParameter(Object parameter) {
        parameters.add(parameter);
    }

    private void initialize(QuerySpec query, TranslationMapping rootModel) {
        query.getFilterExpression().stream()
                .map(criterion -> parseExpression(criterion, rootModel))
                .forEach(conditionExpression -> {
                    whereClauses.add(conditionExpression.toSql());

                    var params = conditionExpression.toStatementParameter().skip(1).toList();
                    parameters.addAll(params);
                });

        orderByClause = parseSortField(query, rootModel);
    }

    private String parseSortField(QuerySpec query, TranslationMapping rootModel) {
        if (query.getSortField() == null) {
            return orderByClause;
        } else {
            var order = query.getSortOrder() == SortOrder.ASC ? "ASC" : "DESC";
            var sortField = rootModel.getStatement(query.getSortField(), String.class);
            if (sortField == null) {
                throw new IllegalArgumentException(format("Cannot sort by %s because the field does not exist", query.getSortField()));
            }
            return String.format(ORDER_BY_TOKEN + " ", sortField, order);
        }
    }

    @NotNull
    private SqlConditionExpression parseExpression(Criterion criterion, TranslationMapping rootModel) {
        var newCriterion = Optional.ofNullable(criterion.getOperandLeft())
                .map(Object::toString)
                .map(it -> rootModel.getStatement(it, criterion.getOperandRight().getClass()))
                .map(criterion::withLeftOperand)
                .orElseGet(() -> Criterion.criterion("0", "=", 1));

        var conditionExpression = new SqlConditionExpression(newCriterion);

        conditionExpression.isValidExpression()
                .orElseThrow(f -> new IllegalArgumentException("This expression is not valid: " + f.getFailureDetail()));

        return conditionExpression;
    }

}
