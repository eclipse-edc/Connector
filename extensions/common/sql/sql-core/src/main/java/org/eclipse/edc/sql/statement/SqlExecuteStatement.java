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

public class SqlExecuteStatement {

    private final List<ColumnEntry> columnEntries = new ArrayList<>();
    private final String jsonCastOperator;

    public SqlExecuteStatement(String jsonCastOperator) {
        this.jsonCastOperator = jsonCastOperator;
    }

    public static SqlExecuteStatement newInstance(String jsonCastOperator) {
        return new SqlExecuteStatement(jsonCastOperator);
    }

    public SqlExecuteStatement add(ColumnEntry columnEntry) {
        columnEntries.add(columnEntry);
        return this;
    }

    public SqlExecuteStatement column(String columnName) {
        columnEntries.add(standardColumn(columnName));
        return this;
    }

    public SqlExecuteStatement jsonColumn(String columnName) {
        columnEntries.add(ColumnEntry.jsonColumn(columnName, jsonCastOperator));
        return this;
    }

    public String insertInto(String tableName) {
        var columnValues = columnEntries.stream().reduce(ColumnEntry::append).orElseThrow();

        return format("INSERT INTO %s (%s) VALUES (%s);", tableName, columnValues.columnName(), columnValues.value());
    }

    public String update(String tableName, ColumnEntry where) {
        var statement = columnEntries.stream()
                .map(it -> it.columnName() + "=" + it.value())
                .collect(joining(","));

        return format("UPDATE %s SET %s WHERE %s=%s", tableName, statement, where.columnName(), where.value());
    }
}
