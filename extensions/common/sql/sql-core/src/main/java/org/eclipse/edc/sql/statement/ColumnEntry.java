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

import static java.lang.String.format;

/**
 * Represent a column and its value into a JDBC prepared statement.
 *
 * @param columnName the name of the column.
 * @param value the value of the column, by default it will be '?' but it could be different.
 */
public record ColumnEntry(String columnName, String value) {

    public static ColumnEntry standardColumn(String columnName) {
        return new ColumnEntry(columnName, "?");
    }

    public static ColumnEntry jsonColumn(String columnName, String jsonOperator) {
        return new ColumnEntry(columnName, "?" + jsonOperator);
    }

    public ColumnEntry append(ColumnEntry other) {
        return new ColumnEntry(columnName + ", " + other.columnName, value + ", " + other.value);
    }

    public String asString() {
        return format("%s = %s", columnName, value);
    }
}
