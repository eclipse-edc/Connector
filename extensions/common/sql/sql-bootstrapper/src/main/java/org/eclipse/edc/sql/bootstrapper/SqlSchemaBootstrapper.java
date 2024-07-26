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

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

public class SqlSchemaBootstrapper {
    private final TransactionContext transactionContext;
    private final QueryExecutor queryExecutor;
    private final List<QueuedStatementRecord> statements = new ArrayList<>();
    private final DataSourceRegistry dataSourceRegistry;

    public SqlSchemaBootstrapper(TransactionContext transactionContext, QueryExecutor queryExecutor, DataSourceRegistry dataSourceRegistry) {

        this.transactionContext = transactionContext;
        this.queryExecutor = queryExecutor;
        this.dataSourceRegistry = dataSourceRegistry;
    }

    public void queueStatement(String datasourceName, String statement, Object... parameters) {
        statements.add(new QueuedStatementRecord(datasourceName, statement, parameters));
    }

    public Result<Void> executeSql() {

        return transactionContext.execute(() -> {
            Stream<Result<Void>> objectStream = statements.stream().map(statement -> {
                var connectionResult = getConnection(statement.datasourceName);
                return connectionResult.compose(connection -> {
                    queryExecutor.execute(connection, statement.sql, statement.parameters);
                    return success();
                });
            });
            return objectStream.reduce(Result::merge).orElse(Result.success());
        });

    }

    public Result<Connection> getConnection(String datasourceName) {
        try {
            return success(dataSourceRegistry.resolve(datasourceName).getConnection());
        } catch (SQLException e) {
            return failure(e.getMessage());
        }
    }

    private record QueuedStatementRecord(String datasourceName, String sql, Object... parameters) {
    }
}
