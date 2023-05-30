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

package org.eclipse.edc.sql;

import java.sql.Connection;
import java.util.stream.Stream;

/**
 * Provides query capabilities to stores
 */
public interface QueryExecutor {

    /**
     * Intended for mutating queries.
     *
     * @param sql the parametrized sql query
     * @param arguments the parameters to interpolate with the parametrized sql query
     * @return rowsChanged
     */
    int execute(Connection connection, String sql, Object... arguments);

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
     */
    <T> Stream<T> query(Connection connection, boolean closeConnection, ResultSetMapper<T> resultSetMapper, String sql, Object... arguments);

    /**
     * Intended for reading queries.
     * Return the first entity that satisfies the query, null if none exists
     *
     * @param connection the connection to be used to execute the query.
     * @param closeConnection if true the connection will be closed on stream closure, else it won't be closed.
     * @param resultSetMapper able to map a row to an object e.g. pojo.
     * @param sql the parametrized sql query
     * @param arguments the parameteres to interpolate with the parametrized sql query
     * @param <T> generic type returned after mapping from the executed query
     * @return the first entity that satisfies the query, null if none exists
     */
    <T> T single(Connection connection, boolean closeConnection, ResultSetMapper<T> resultSetMapper, String sql, Object... arguments);
}
