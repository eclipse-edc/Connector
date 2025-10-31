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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.sql.statement.SqlExecuteStatement.equalTo;
import static org.eclipse.edc.sql.statement.SqlExecuteStatement.isNull;

class SqlExecuteStatementTest {

    @Nested
    class Insert {

        @Test
        void shouldThrowException_whenNoColumnSpecified() {
            assertThatThrownBy(() -> SqlExecuteStatement.newInstance("::json").insertInto("table_name"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldReturnStatement_whenThereAreSimpleColumns() {
            var statement = SqlExecuteStatement.newInstance("::json")
                    .column("column_name")
                    .column("another_column_name")
                    .insertInto("table_name");

            assertThat(statement).isEqualToIgnoringCase("insert into table_name (column_name, another_column_name) values (?, ?);");
        }

        @Test
        void shouldReturnStatement_whenJsonColumn() {
            var statement = SqlExecuteStatement.newInstance("::json")
                    .jsonColumn("column_name")
                    .insertInto("table_name");

            assertThat(statement).isEqualToIgnoringCase("insert into table_name (column_name) values (?::json);");
        }
    }

    @Nested
    class Update {

        @Test
        void shouldThrowException_whenNoColumnSpecified() {
            assertThatThrownBy(() -> SqlExecuteStatement.newInstance("::json")
                    .update("table_name", equalTo("id")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldReturnStatement_whenThereAreSimpleColumns() {
            var statement = SqlExecuteStatement.newInstance("::json")
                    .column("column_name")
                    .column("another_column_name")
                    .update("table_name", equalTo("id"));

            assertThat(statement).isEqualToIgnoringCase("update table_name set column_name = ?, another_column_name = ? where id = ?;");
        }

        @Test
        void shouldReturnStatement_whenJsonColumn() {
            var statement = SqlExecuteStatement.newInstance("::json")
                    .jsonColumn("column_name")
                    .update("table_name", equalTo("id"));

            assertThat(statement).isEqualToIgnoringCase("update table_name set column_name = ?::json where id = ?;");
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldReturnStatement() {
            var statement = SqlExecuteStatement.newInstance("::json")
                    .delete("table_name", equalTo("id"));

            assertThat(statement).isEqualToIgnoringCase("delete from table_name where id = ?;");
        }

        @Test
        void shouldReturnStatementWithAndOperator_whenMultipleWhereClauses() {
            var statement = SqlExecuteStatement.newInstance("::json")
                    .delete("table_name", equalTo("id"), isNull("field"));

            assertThat(statement).isEqualToIgnoringCase("delete from table_name where id = ? and field is null;");
        }
    }

    @Nested
    class Upsert {

        @Test
        void shouldThrowException_whenNoColumnSpecified() {
            assertThatThrownBy(() -> SqlExecuteStatement.newInstance("::json").upsertInto("table_name", "id"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldReturnStatement_whenThereAreSimpleColumns() {
            var statement = SqlExecuteStatement.newInstance("::json")
                    .column("id")
                    .column("column_name")
                    .column("another_column_name")
                    .upsertInto("table_name", "id");

            assertThat(statement).isEqualToIgnoringCase("insert into table_name (id, column_name, another_column_name) values (?, ?, ?)" +
                    " on conflict (id) do update set column_name = excluded.column_name, another_column_name = excluded.another_column_name;");
        }

        @Test
        void shouldReturnStatement_whenThereWhereConditions() {
            var statement = SqlExecuteStatement.newInstance("::json")
                    .column("id")
                    .column("column_name")
                    .column("another_column_name")
                    .upsertInto("table_name", "id", "another_column_name = ?");

            assertThat(statement).isEqualToIgnoringCase("insert into table_name (id, column_name, another_column_name) values (?, ?, ?)" +
                    " on conflict (id) do update set column_name = excluded.column_name, another_column_name = excluded.another_column_name" +
                    " WHERE another_column_name = ?;");
        }

        @Test
        void shouldReturnStatement_whenSkipColumns() {
            var statement = SqlExecuteStatement.newInstance("::json")
                    .column("id")
                    .column("column_name")
                    .column("another_column_name")
                    .upsertInto("table_name", "id", List.of("another_column_name"));

            assertThat(statement).isEqualToIgnoringCase("insert into table_name (id, column_name, another_column_name) values (?, ?, ?)" +
                    " on conflict (id) do update set column_name = excluded.column_name;");
        }

        @Test
        void shouldReturnStatement_whenJsonColumn() {
            var statement = SqlExecuteStatement.newInstance("::json")
                    .column("id")
                    .jsonColumn("column_name")
                    .upsertInto("table_name", "id");

            assertThat(statement).isEqualToIgnoringCase("insert into table_name (id, column_name) values (?, ?::json)" +
                    " on conflict (id) do update set column_name = excluded.column_name;");
        }
    }
}
