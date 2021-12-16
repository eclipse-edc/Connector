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

import org.eclipse.dataspaceconnector.clients.postgresql.row.RowMapper;

import java.sql.SQLException;
import java.util.List;

/**
 * Client capable of issuing SQL queries against postgresql databases
 */
public interface PostgresqlClient extends AutoCloseable {

    /**
     * Intended for mutating queries.
     *
     * @param sql       the parametrized sql query
     * @param arguments the parameteres to interpolate with the parametrized sql query
     * @return rowsChanged
     * @throws SQLException if execution of the query was failing
     */
    int execute(String sql, Object... arguments) throws SQLException;

    /**
     * Intended for reading queries.
     *
     * @param rowMapper able to map a row to an object e.g. pojo.
     * @param sql       the parametrized sql query
     * @param arguments the parameteres to interpolate with the parametrized sql query
     * @param <T>       generic type returned after mapping from the executed query
     * @return results
     * @throws SQLException if execution of the query or mapping was failing
     */
    <T> List<T> execute(RowMapper<T> rowMapper, String sql, Object... arguments) throws SQLException;

    /**
     * Spans a transaction around the passed callback.
     *
     * @param callback transaction scoped callback
     * @throws SQLException if the transaction was failing
     */
    void doInTransaction(TransactionCallback callback) throws SQLException;

    /**
     * Closes the postgres client and frees all underlying resources.
     *
     * @throws Exception in case an error was encountered while closing the postgres client
     */
    default void close() throws Exception {
    }

    /**
     * A transaction scoped callback
     */
    @FunctionalInterface
    interface TransactionCallback {

        /**
         * Callback.
         *
         * @param postgresqlClient the transaction scoped postgres client
         * @throws SQLException if execution of any operations encountered an error
         */
        void accept(PostgresqlClient postgresqlClient) throws SQLException;
    }
}