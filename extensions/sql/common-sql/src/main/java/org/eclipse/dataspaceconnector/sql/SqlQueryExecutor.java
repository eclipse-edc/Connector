/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.sql;

import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * The SqlQueryExecutor is capable of executing parametrized SQL queries
 */
public final class SqlQueryExecutor {

    private SqlQueryExecutor() {
    }

    /**
     * Intended for mutating queries.
     *
     * @param sql       the parametrized sql query
     * @param arguments the parameters to interpolate with the parametrized sql query
     * @return rowsChanged
     */
    public static int executeQuery(Connection connection, String sql, Object... arguments) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(arguments, "arguments");

        try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            setArguments(statement, arguments);
            return statement.execute() ? 0 : statement.getUpdateCount();
        } catch (Exception exception) {
            throw new EdcPersistenceException(exception.getMessage(), exception);
        }
    }

    /**
     * Intended for reading queries.
     *
     * @param resultSetMapper able to map a row to an object e.g. pojo.
     * @param sql             the parametrized sql query
     * @param arguments       the parameteres to interpolate with the parametrized sql query
     * @param <T>             generic type returned after mapping from the executed query
     * @return results
     */
    public static <T> List<T> executeQuery(Connection connection, ResultSetMapper<T> resultSetMapper, String sql, Object... arguments) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(resultSetMapper, "resultSetMapper");
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(arguments, "arguments");

        try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            setArguments(statement, arguments);
            return statement.execute() ? mapResultSet(statement.getResultSet(), resultSetMapper) : Collections.emptyList();
        } catch (Exception exception) {
            throw new EdcPersistenceException(exception.getMessage(), exception);
        }
    }

    private static void setArguments(PreparedStatement statement, Object[] arguments) throws SQLException {
        for (int index = 0; index < arguments.length; index++) {
            int position = index + 1;
            setArgument(statement, position, arguments[index]);
        }
    }

    private static void setArgument(PreparedStatement statement, int position, Object argument) throws SQLException {
        ArgumentHandler argumentHandler = findArgumentHandler(argument);

        if (argumentHandler != null) {
            argumentHandler.handle(statement, position, argument);
            return;
        }

        statement.setObject(position, argument);
    }

    private static ArgumentHandler findArgumentHandler(Object argument) {
        for (ArgumentHandler handler : ArgumentHandlers.values()) {
            if (handler.accepts(argument)) {
                return handler;
            }
        }

        return null;
    }

    private static <T> List<T> mapResultSet(ResultSet resultSet, ResultSetMapper<T> resultSetMapper) throws Exception {
        List<T> results = new LinkedList<>();

        if (resultSet != null) {
            while (resultSet.next()) {
                results.add(resultSetMapper.mapResultSet(resultSet));
            }
        }

        return results;
    }
}
