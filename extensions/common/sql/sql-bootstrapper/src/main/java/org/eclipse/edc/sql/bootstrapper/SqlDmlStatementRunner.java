/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.sql.bootstrapper;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Internal class to the SQL Bootstrapper Extension module with the intended purpose to execute a series of DML statements against
 * the database.
 */
class SqlDmlStatementRunner {

    private final TransactionContext transactionContext;
    private final QueryExecutor queryExecutor;
    private final Monitor monitor;
    private final DataSourceRegistry dataSourceRegistry;

    SqlDmlStatementRunner(TransactionContext transactionContext, QueryExecutor queryExecutor, Monitor monitor, DataSourceRegistry dataSourceRegistry) {
        this.transactionContext = transactionContext;
        this.queryExecutor = queryExecutor;
        this.monitor = monitor;
        this.dataSourceRegistry = dataSourceRegistry;
    }

    /**
     * Executes the queued DML statements one after the other. This method is intended to be called only from the {@link SqlSchemaBootstrapperExtension}.
     *
     * @param statements A map containing the datasource name as key and the SQL statements as value
     * @return A summary result of all the statements.
     */
    public Result<Void> executeSql(Map<String, List<String>> statements) {
        monitor.debug("Running DML statements: [%s]".formatted(String.join(", ", statements.keySet())));
        return transactionContext.execute(() -> statements.entrySet().stream()
                .map(statement -> {
                    var connectionResult = getConnection(statement.getKey());
                    return connectionResult.compose(connection -> {
                        try {
                            queryExecutor.execute(connection, String.join("", statement.getValue()));
                        } catch (EdcPersistenceException sqlException) {
                            return failure(sqlException.getMessage());
                        }
                        return success();
                    });
                })
                .reduce(Result::merge)
                .orElse(Result.success()));
    }

    public Result<Connection> getConnection(String datasourceName) {
        try {
            var resolve = dataSourceRegistry.resolve(datasourceName);
            return resolve != null ? success(resolve.getConnection()) :
                    failure("No datasource found with name '%s'".formatted(datasourceName));
        } catch (SQLException e) {
            return failure(e.getMessage());
        }
    }
}
