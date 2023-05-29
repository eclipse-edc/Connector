/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.sql;

import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;

/**
 * The SqlQueryExecutor is capable of executing parametrized SQL queries
 */
public class SqlQueryExecutor implements QueryExecutor {

    /**
     * static INSTANCE provided only to make the static methods working.
     *
     * @deprecated please inject and use the {@link QueryExecutor} service instance instead.
     */
    @Deprecated(since = "0.1.0")
    private static final SqlQueryExecutor INSTANCE = new SqlQueryExecutor();
    private final SqlQueryExecutorConfiguration configuration;

    public SqlQueryExecutor() {
        this(SqlQueryExecutorConfiguration.ofDefaults());
    }

    public SqlQueryExecutor(SqlQueryExecutorConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public int execute(Connection connection, String sql, Object... arguments) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(arguments, "arguments");

        try (var statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            setArguments(statement, arguments);
            return statement.execute() ? 0 : statement.getUpdateCount();
        } catch (Exception exception) {
            throw new EdcPersistenceException(exception.getMessage(), exception);
        }
    }

    @Override
    public <T> T single(Connection connection, boolean closeConnection, ResultSetMapper<T> resultSetMapper, String sql, Object... arguments) {
        try (var stream = query(connection, closeConnection, resultSetMapper, sql, arguments)) {
            return stream.findFirst().orElse(null);
        }
    }

    @Override
    public <T> Stream<T> query(Connection connection, boolean closeConnection, ResultSetMapper<T> resultSetMapper, String sql, Object... arguments) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(resultSetMapper, "resultSetMapper");
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(arguments, "arguments");

        var doorKeeper = new DoorKeeper();
        try {
            if (closeConnection) {
                doorKeeper.takeCareOf(connection);
            }
            var statement = connection.prepareStatement(sql);
            doorKeeper.takeCareOf(statement);
            statement.setFetchSize(configuration.fetchSize());
            setArguments(statement, arguments);
            var resultSet = statement.executeQuery();
            doorKeeper.takeCareOf(resultSet);
            var splititerator = createSpliterator(resultSetMapper, resultSet);
            return stream(splititerator, false).onClose(doorKeeper::close);
        } catch (SQLException sqlEx) {
            try {
                doorKeeper.close();
            } catch (Exception ex) {
                sqlEx.addSuppressed(ex);
            }

            throw new EdcPersistenceException(sqlEx);
        }
    }

    private void setArguments(PreparedStatement statement, Object[] arguments) throws SQLException {
        for (int index = 0; index < arguments.length; index++) {
            int position = index + 1;
            setArgument(statement, position, arguments[index]);
        }
    }

    private void setArgument(PreparedStatement statement, int position, Object argument) throws SQLException {
        var argumentHandler = Arrays.stream(ArgumentHandlers.values()).filter(it -> it.accepts(argument))
                .findFirst().orElse(null);

        if (argumentHandler != null) {
            argumentHandler.handle(statement, position, argument);
        } else {
            statement.setObject(position, argument);
        }
    }

    @NotNull
    private <T> Spliterators.AbstractSpliterator<T> createSpliterator(ResultSetMapper<T> resultSetMapper, ResultSet resultSet) {
        return new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED) {
            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                try {
                    if (!resultSet.next()) {
                        return false;
                    }
                    action.accept(resultSetMapper.mapResultSet(resultSet));
                    return true;
                } catch (Exception ex) {
                    throw new EdcPersistenceException(ex);
                }
            }

        };
    }

    /**
     * Intended for mutating queries.
     *
     * @param sql the parametrized sql query
     * @param arguments the parameters to interpolate with the parametrized sql query
     * @return rowsChanged
     * @deprecated please inject and use the {@link QueryExecutor} service instance instead
     */
    @Deprecated(since = "0.1.0")
    public static int executeQuery(Connection connection, String sql, Object... arguments) {
        return INSTANCE.execute(connection, sql, arguments);
    }

    @Deprecated(since = "0.1.0")
    public static <T> T executeQuerySingle(Connection connection, boolean closeConnection, ResultSetMapper<T> resultSetMapper, String sql, Object... arguments) {
        return INSTANCE.single(connection, closeConnection, resultSetMapper, sql, arguments);
    }

    /**
     * Intended for reading queries.
     * The resulting {@link Stream} must be closed with the "close()" when a terminal operation is used on the stream
     * (collect, forEach, anyMatch, etc...)
     *
     * @param connection the connection to be used to execute the query.
     * @param closeConnection if true the connection will be closed on stream closure, else it won't be closed.
     * @param resultSetMapper able to map a row to an object e.g. pojo.
     * @param sql the parametrized sql query
     * @param arguments the parameteres to interpolate with the parametrized sql query
     * @param <T> generic type returned after mapping from the executed query
     * @return a Stream on the results, must be closed when a terminal operation is used on the stream (collect, forEach, anyMatch, etc...)
     * @deprecated please inject and use the {@link QueryExecutor} service instance instead
     */
    @Deprecated(since = "0.1.0")
    public static <T> Stream<T> executeQuery(Connection connection, boolean closeConnection, ResultSetMapper<T> resultSetMapper, String sql, Object... arguments) {
        return INSTANCE.query(connection, closeConnection, resultSetMapper, sql, arguments);
    }

}
