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

package org.eclipse.dataspaceconnector.clients.postgresql.connection.pool;

import java.sql.SQLException;

/**
 * The connection pool maintains a cache of database connections,
 * which can be reused when future requests to a database are needed.
 */
public interface ConnectionPool extends AutoCloseable {

    /**
     * Connection is guarded by the providing connection pool.
     * Once the callback method returns the provided connection
     * is returned and made available to be used by others.
     *
     * @param callback to be acting on the provided connection
     * @param <T>      generic result argument
     * @return result
     * @throws SQLException if something went wrong
     */
    <T> T doWithConnection(ConnectionCallback<T> callback) throws SQLException;
}
