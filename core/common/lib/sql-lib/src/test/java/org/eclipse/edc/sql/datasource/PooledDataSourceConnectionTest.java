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
 *       Daimler TSS GmbH - Initial Test
 *
 */

package org.eclipse.edc.sql.datasource;

import org.eclipse.edc.sql.pool.ConnectionPool;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.SQLException;

class PooledDataSourceConnectionTest {

    @Test
    void closeReturnsConnectionToPool() throws SQLException {
        Connection delegate = Mockito.mock(Connection.class);
        ConnectionPool connectionPool = Mockito.mock(ConnectionPool.class);

        PooledDataSourceConnection pooledDataSourceConnection = new PooledDataSourceConnection(delegate, connectionPool);

        pooledDataSourceConnection.close();

        Mockito.verify(connectionPool, Mockito.times(1)).returnConnection(delegate);
    }
}
