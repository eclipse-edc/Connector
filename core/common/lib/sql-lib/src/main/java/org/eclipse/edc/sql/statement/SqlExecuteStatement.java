/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.sql.statement;

import org.eclipse.edc.spi.query.Criterion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.sql.statement.ColumnEntry.standardColumn;

/**
 * Helps in constructing SQL insert/update statements without having to deal with the SQL syntax directly
 */
public class SqlExecuteStatement {

    private final List<ColumnEntry> columnEntries = new ArrayList<>();
    private final String jsonCastOperator;

    public SqlExecuteStatement(String jsonCastOperator) {
        this.jsonCastOperator = jsonCastOperator;
    }

    /**
     * Create a new instance with the JSON cast operator
     *
     * @param jsonCastOperator the json cast operator.
     * @return a new {@link SqlExecuteStatement}.
     */
    public static SqlExecuteStatement newInstance(String jsonCastOperator) {
        return new SqlExecuteStatement(jsonCastOperator);
    }

    /**
     * Utility criterion to describe an equality condition in a SQL prepared statement.
     *
     * @param columnName the column name.
     * @return the criterion.
     */
    public static Criterion equalTo(String columnName) {
        return criterion(columnName, "=", "?");
    }

    /**
     * Utility criterion to describe an "is null" condition in a SQL prepared statement.
     *
     * @param columnName the column name.
     * @return the criterion.
     */
    public static Criterion isNull(String columnName) {
        return criterion(columnName, "is", "null");
    }

    /**
     * Add a new standard column
     *
     * @param columnName the column name.
     * @return the {@link SqlExecuteStatement}.
     */
    public SqlExecuteStatement column(String columnName) {
        columnEntries.add(standardColumn(columnName));
        return this;
    }

    /**
     * Add a new json column
     *
     * @param columnName the column name.
     * @return the {@link SqlExecuteStatement}.
     */
    public SqlExecuteStatement jsonColumn(String columnName) {
        columnEntries.add(ColumnEntry.jsonColumn(columnName, jsonCastOperator));
        return this;
    }

    /**
     * Gives a SQL insert statement.
     *
     * @param tableName the table name.
     * @return sql insert statement.
     */
    public String insertInto(String tableName) {
        if (columnEntries.isEmpty()) {
            throw new IllegalArgumentException(format("Cannot create INSERT statement on %s because no columns are registered", tableName));
        }

        return insertStatement(tableName) + ";";
    }

    /**
     * Gives a SQL update statement.
     *
     * @param tableName   the table name.
     * @param whereColumn the column that will be used for the where condition
     * @return sql update statement.
     */
    public String update(String tableName, String whereColumn) {
        return update(tableName, criterion(whereColumn, "=", "?"));
    }

    /**
     * Gives a SQL update statement.
     *
     * @param tableName the table name.
     * @param where     the update field criterion
     * @return sql update statement.
     */
    public String update(String tableName, Criterion where) {
        if (columnEntries.isEmpty()) {
            throw new IllegalArgumentException(format("Cannot create UPDATE statement on %s because no columns are registered", tableName));
        }

        var statement = columnEntries.stream()
                .map(ColumnEntry::asString)
                .collect(joining(", "));

        return format("UPDATE %s SET %s WHERE %s;", tableName, statement, where);
    }

    /**
     * Gives a SQL delete statement.
     *
     * @param tableName   the table name.
     * @param whereColumn the column that will be used for the where condition
     * @return sql delete statement.
     */
    public String delete(String tableName, String whereColumn) {
        return delete(tableName, criterion(whereColumn, "=", "?"));
    }

    /**
     * Gives a SQL delete statement. The where criteria is joined with AND operator.
     *
     * @param tableName     the table name.
     * @param whereCriteria the delete field condition
     * @return sql delete statement.
     */
    public String delete(String tableName, Criterion... whereCriteria) {
        var where = Arrays.stream(whereCriteria)
                .map(Criterion::toString)
                .collect(joining(" AND "));

        return format("DELETE FROM %s WHERE %s;", tableName, where);
    }

    /**
     * Gives a SQL upsert statement based on the "ON CONFLICT" semantic
     *
     * @param tableName the table name.
     * @param idColumn  the id column.
     * @return sql upsert statement.
     */
    public String upsertInto(String tableName, String idColumn) {
        return upsertInto(tableName, idColumn, List.of(), null);
    }

    /**
     * Gives a SQL upsert statement based on the "ON CONFLICT" semantic
     *
     * @param tableName   the table name.
     * @param idColumn    the id column.
     * @param whereFilter additional filter to add to the update on conflict clause
     * @return sql upsert statement.
     */
    public String upsertInto(String tableName, String idColumn, String whereFilter) {
        return upsertInto(tableName, idColumn, List.of(), whereFilter);
    }

    /**
     * Gives a SQL upsert statement based on the "ON CONFLICT" semantic
     *
     * @param tableName   the table name.
     * @param idColumn    the id column.
     * @param skipColumns the columns to skip during update.
     * @return sql upsert statement.
     */
    public String upsertInto(String tableName, String idColumn, List<String> skipColumns) {
        return upsertInto(tableName, idColumn, skipColumns, null);
    }

    /**
     * Gives a SQL upsert statement based on the "ON CONFLICT" semantic
     *
     * @param tableName   the table name.
     * @param idColumn    the id column.
     * @param skipColumns the columns to skip during update.
     * @param whereFilter additional filter to add to the update on conflict clause
     * @return sql upsert statement.
     */
    public String upsertInto(String tableName, String idColumn, List<String> skipColumns, String whereFilter) {
        var toSkip = new HashSet<>(skipColumns);
        toSkip.add(idColumn);
        if (columnEntries.isEmpty()) {
            throw new IllegalArgumentException(format("Cannot create UPSERT statement on %s because no columns are registered", tableName));
        }

        var updateFields = columnEntries.stream()
                .map(ColumnEntry::columnName)
                .filter(it -> !toSkip.contains(it))
                .map(it -> it + " = EXCLUDED." + it)
                .collect(joining(", "));

        var whereClause = whereFilter != null ? " WHERE " + whereFilter : "";

        return insertStatement(tableName) + " ON CONFLICT (" + idColumn + ") DO UPDATE SET " + updateFields + whereClause + ";";
    }

    private String insertStatement(String tableName) {
        var columnValues = columnEntries.stream().reduce(ColumnEntry::append).orElseThrow();

        return "INSERT INTO " + tableName + " (" + columnValues.columnName() + ") VALUES (" + columnValues.value() + ")";
    }
}
