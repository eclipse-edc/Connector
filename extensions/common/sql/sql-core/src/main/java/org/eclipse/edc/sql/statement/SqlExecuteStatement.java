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

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
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
     * Add a new columnEntry
     *
     * @param columnEntry the columnEntry.
     * @return the {@link SqlExecuteStatement}.
     */
    public SqlExecuteStatement add(ColumnEntry columnEntry) {
        columnEntries.add(columnEntry);
        return this;
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
        var columnValues = columnEntries.stream().reduce(ColumnEntry::append).orElseThrow();

        return format("INSERT INTO %s (%s) VALUES (%s);", tableName, columnValues.columnName(), columnValues.value());
    }


    /**
     * Gives a SQL update statement.
     *
     * @param tableName the table name.
     * @param where the update field condition
     * @return sql update statement.
     */
    public String update(String tableName, ColumnEntry where) {
        var statement = columnEntries.stream()
                .map(it -> it.columnName() + "=" + it.value())
                .collect(joining(","));

        return format("UPDATE %s SET %s WHERE %s=%s", tableName, statement, where.columnName(), where.value());
    }
}
