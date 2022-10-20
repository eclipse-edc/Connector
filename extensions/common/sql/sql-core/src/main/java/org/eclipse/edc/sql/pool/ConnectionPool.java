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

package org.eclipse.edc.sql.pool;

import java.sql.Connection;

/**
 * The ConnectionPool maintains a cache of reusable {@link java.sql.Connection}s.
 */
public interface ConnectionPool extends AutoCloseable {

    /**
     * Retrieves a connection managed by the pool.
     *
     * @return connection to be exclusively used until returned to the pool
     */
    Connection getConnection();

    /**
     * Returns a provided connection back to the pool and thus makes
     * it available to be used by other consumers.
     *
     * @param connection to be returned to the pool
     */
    void returnConnection(Connection connection);
}
