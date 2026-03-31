/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.controlplane.tasks.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.tasks.TaskPayload;
import org.eclipse.edc.controlplane.tasks.store.TaskStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * SQL implementation of {@link TaskStore}.
 */
public class SqlTaskStore extends AbstractSqlStore implements TaskStore {

    private final TaskStatements statements;

    public SqlTaskStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext, ObjectMapper objectMapper, QueryExecutor queryExecutor, TaskStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public StoreResult<Void> create(Task task) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var stmt = statements.getInsertTemplate();
                queryExecutor.execute(connection, stmt,
                        task.getId(),
                        task.getName(),
                        task.getGroup(),
                        toJson(task.getPayload()),
                        task.getRetryCount(),
                        task.getAt()
                );
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> update(Task task) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var stmt = statements.getUpdateTemplate();
                var count = queryExecutor.execute(connection, stmt,
                        task.getName(),
                        task.getGroup(),
                        toJson(task.getPayload()),
                        task.getRetryCount(),
                        task.getAt(),
                        task.getId()
                );
                if (count == 0) {
                    return StoreResult.notFound("Task with id %s not found".formatted(task.getId()));
                }
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public List<Task> fetchForUpdate(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var query = statements.createQuery(querySpec).forUpdate(true);
                return queryExecutor.query(connection, true, this::mapResultSet, query.getQueryAsString(), query.getParameters()).toList();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public Task findById(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var stmt = statements.findByIdTemplate();
                return queryExecutor.single(connection, true, this::mapResultSet, stmt, id);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> delete(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var stmt = statements.getDeleteStatement();
                var count = queryExecutor.execute(connection, stmt, id);
                if (count == 0) {
                    return StoreResult.notFound("Task with id %s not found".formatted(id));
                }
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private Task mapResultSet(ResultSet resultSet) throws Exception {

        var payload = fromJson(resultSet.getString(statements.getPayloadColumn()), TaskPayload.class);
        return Task.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .payload(payload)
                .name(resultSet.getString(statements.getNameColumn()))
                .group(resultSet.getString(statements.getGroupColumn()))
                .retryCount(resultSet.getInt(statements.getRetryCountColumn()))
                .at(resultSet.getLong(statements.getTimestampColumn()))
                .build();
    }
}
