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

package org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.commons;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.ConnectionFactory;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.ConnectionCallback;
import org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.ConnectionPool;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

public final class CommonsConnectionPool implements ConnectionPool, AutoCloseable {
    private final GenericObjectPool<Connection> connectionObjectPool;

    public CommonsConnectionPool(ConnectionFactory connectionFactory, CommonsConnectionPoolConfig commonsConnectionPoolConfig) {
        Objects.requireNonNull(connectionFactory, "connectionFactory");
        Objects.requireNonNull(commonsConnectionPoolConfig, "commonsConnectionPoolConfig");

        this.connectionObjectPool = new GenericObjectPool<>(
                new PooledConnectionObjectFactory(connectionFactory, commonsConnectionPoolConfig.getTestQuery()),
                getGenericObjectPoolConfig(commonsConnectionPoolConfig));
    }

    private static GenericObjectPoolConfig<Connection> getGenericObjectPoolConfig(CommonsConnectionPoolConfig commonsConnectionPoolConfig) {
        GenericObjectPoolConfig<Connection> genericObjectPoolConfig = new GenericObjectPoolConfig<>();

        // no need for JMX
        genericObjectPoolConfig.setJmxEnabled(false);

        genericObjectPoolConfig.setMaxIdle(commonsConnectionPoolConfig.getMaxIdleConnections());
        genericObjectPoolConfig.setMaxTotal(commonsConnectionPoolConfig.getMaxTotalConnections());
        genericObjectPoolConfig.setMinIdle(commonsConnectionPoolConfig.getMinIdleConnections());

        genericObjectPoolConfig.setTestOnBorrow(commonsConnectionPoolConfig.getTestConnectionOnBorrow());
        genericObjectPoolConfig.setTestOnCreate(commonsConnectionPoolConfig.getTestConnectionOnCreate());
        genericObjectPoolConfig.setTestOnReturn(commonsConnectionPoolConfig.getTestConnectionOnReturn());
        genericObjectPoolConfig.setTestWhileIdle(commonsConnectionPoolConfig.getTestConnectionWhileIdle());

        return genericObjectPoolConfig;
    }

    public CommonsConnectionPoolStats getStats() {
        return CommonsConnectionPoolStats.Builder.newInstance()
                .active(connectionObjectPool.getNumActive())
                .idle(connectionObjectPool.getNumIdle())
                .waiters(connectionObjectPool.getNumWaiters())
                .build();
    }

    @Override
    public <T> T doWithConnection(ConnectionCallback<T> callback) throws SQLException {
        Objects.requireNonNull(callback, "callback");

        Connection connection;
        try {
            connection = connectionObjectPool.borrowObject();
        } catch (SQLException sqlException) {
            throw sqlException;
        } catch (Exception e) {
            throw new SQLException(e);
        }

        T result = null;
        SQLException sqlException = null;
        try {
            result = callback.apply(connection);

            connectionObjectPool.returnObject(connection);
        } catch (Exception exception) {
            try {
                connectionObjectPool.invalidateObject(connection);
            } catch (Exception e) {
                // ignore
            }

            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else if (exception instanceof SQLException) {
                sqlException = (SQLException) exception;
            } else {
                sqlException = new SQLException(exception);
            }
        }

        if (sqlException != null) {
            throw sqlException;
        }

        return result;
    }

    @Override
    public void close() {
        connectionObjectPool.close();
    }

    private static class PooledConnectionObjectFactory extends BasePooledObjectFactory<Connection> {
        private final String testQuery;
        private final ConnectionFactory connectionFactory;

        public PooledConnectionObjectFactory(@NotNull ConnectionFactory connectionFactory, @NotNull String testQuery) {
            this.connectionFactory = Objects.requireNonNull(connectionFactory);
            this.testQuery = Objects.requireNonNull(testQuery);
        }

        @Override
        public Connection create() throws SQLException {
            return connectionFactory.create();
        }

        @Override
        public void destroyObject(PooledObject<Connection> pooledObject, DestroyMode destroyMode) throws Exception {
            if (pooledObject == null) {
                return;
            }

            Connection connection = pooledObject.getObject();

            if (connection != null && !connection.isClosed()) {
                connection.close();
            }

            pooledObject.invalidate();
        }

        @Override
        public boolean validateObject(PooledObject<Connection> pooledObject) {
            if (pooledObject == null) {
                return false;
            }

            Connection connection = pooledObject.getObject();
            if (connection == null) {
                return false;
            }

            return isConnectionValid(connection);
        }

        private boolean isConnectionValid(Connection connection) {
            try {
                if (connection.isClosed()) {
                    return false;
                }

                try (PreparedStatement preparedStatement = connection.prepareStatement(testQuery)) {
                    return preparedStatement.execute();
                } catch (SQLException e) {
                    return false;
                }
            } catch (SQLException sqlException) {
                return false;
            }
        }

        @Override
        public PooledObject<Connection> wrap(Connection connection) {
            return new DefaultPooledObject<>(connection);
        }
    }
}
