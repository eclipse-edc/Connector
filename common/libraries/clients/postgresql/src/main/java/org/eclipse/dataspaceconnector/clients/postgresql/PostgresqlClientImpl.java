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

package org.eclipse.dataspaceconnector.clients.postgresql;

import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.ConnectionPool;
import org.eclipse.dataspaceconnector.clients.postgresql.row.RowMapper;
import org.eclipse.dataspaceconnector.clients.postgresql.statement.ArgumentHandler;
import org.eclipse.dataspaceconnector.clients.postgresql.statement.ArgumentHandlers;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * An implementation of a {@link org.eclipse.dataspaceconnector.clients.postgresql.PostgresqlClient}
 * backed by an {@link org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.ConnectionPool}
 */
public class PostgresqlClientImpl implements PostgresqlClient {
    private final ConnectionPool connectionPool;

    /**
     * Constructor for instantiating the PostgresqlClientImpl
     *
     * @param connectionPool mandatory for obtaining connections
     */
    public PostgresqlClientImpl(ConnectionPool connectionPool) {
        Objects.requireNonNull(connectionPool, "connectionFactory");

        this.connectionPool = connectionPool;
    }

    @Override
    public int execute(String sql, Object... arguments) throws SQLException {
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(arguments, "arguments");

        return connectionPool.doWithConnection(
                connection -> new SingleConnectionScopedPostgresqlClient(connection).execute(sql, arguments));
    }

    @Override
    public <T> List<T> execute(RowMapper<T> rowMapper, String sql, Object... arguments) throws SQLException {
        Objects.requireNonNull(sql, "rowMapper");
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(arguments, "arguments");

        return connectionPool.doWithConnection(
                connection -> new SingleConnectionScopedPostgresqlClient(connection).execute(rowMapper, sql, arguments));
    }

    @Override
    public void doInTransaction(TransactionCallback callback) throws SQLException {
        Objects.requireNonNull(callback, "callback");

        connectionPool.doWithConnection(connection -> {
            doInTransaction(connection, callback);
            return null;
        });
    }

    public void close() throws Exception {
        connectionPool.close();
    }

    private static void doInTransaction(Connection connection, TransactionCallback transactionCallback) throws SQLException {
        new Transaction(connection, transactionCallback).execute();
    }

    private static class SingleConnectionScopedPostgresqlClient implements PostgresqlClient {
        private final Connection connection;

        public SingleConnectionScopedPostgresqlClient(@NotNull Connection connection) {
            this.connection = Objects.requireNonNull(connection, "connection");
        }

        @Override
        public int execute(String sql, Object... arguments) throws SQLException {
            Objects.requireNonNull(sql, "sql");
            Objects.requireNonNull(arguments, "arguments");

            try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

                setArguments(statement, arguments);

                return statement.execute() ? 0 : statement.getUpdateCount();
            }
        }

        @Override
        public <T> List<T> execute(RowMapper<T> rowMapper, String sql, Object... arguments) throws SQLException {
            Objects.requireNonNull(rowMapper, "rowMapper");
            Objects.requireNonNull(sql, "sql");
            Objects.requireNonNull(arguments, "arguments");

            try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

                setArguments(statement, arguments);

                return statement.execute() ? mapRows(statement.getResultSet(), rowMapper) : Collections.emptyList();
            }
        }

        @Override
        public void doInTransaction(@NotNull TransactionCallback callback) throws SQLException {
            throw new SQLException("Transaction already open");
        }

        private void setArguments(PreparedStatement statement, Object[] arguments) throws SQLException {
            for (int index = 0; index < arguments.length; index++) {
                int position = index + 1;

                setArgument(statement, position, arguments[index]);
            }
        }

        private void setArgument(PreparedStatement statement, int position, Object argument) throws SQLException {
            ArgumentHandler argumentHandler = null;
            for (ArgumentHandler handler : ArgumentHandlers.values()) {
                if (handler.accepts(argument)) {
                    argumentHandler = handler;
                    break;
                }
            }

            // fallback: set as object
            if (argumentHandler == null) {
                argumentHandler = new ArgumentHandler() {
                    @Override
                    public boolean accepts(Object value) {
                        return true;
                    }

                    @Override
                    public void handle(PreparedStatement statement, int position, Object argument) throws SQLException {
                        statement.setObject(position, argument);
                    }
                };
            }

            argumentHandler.handle(statement, position, argument);
        }

        private <T> List<T> mapRows(ResultSet resultSet, RowMapper<T> rowMapper) throws SQLException {
            List<T> results = new LinkedList<>();

            if (resultSet != null) {
                while (resultSet.next()) {
                    results.add(rowMapper.mapRow(resultSet));
                }
            }

            return results;
        }
    }

    private static class Transaction {
        private final Connection connection;
        private final TransactionCallback transactionCallback;

        public Transaction(Connection connection, TransactionCallback transactionCallback) {
            this.connection = connection;
            this.transactionCallback = transactionCallback;
        }

        public void execute() throws SQLException {
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("BEGIN");
                }

                transactionCallback.accept(new SingleConnectionScopedPostgresqlClient(connection));

                connection.commit();
            } catch (Exception exception) {
                SQLException sqlException;
                if (exception instanceof SQLException) {
                    sqlException = (SQLException) exception;
                } else {
                    sqlException = new SQLException(exception);
                }

                try {
                    connection.rollback();
                } catch (SQLException txSqlException) {
                    txSqlException.addSuppressed(sqlException);
                    sqlException = txSqlException;
                }

                throw sqlException;
            }
        }
    }
}
