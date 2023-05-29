/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.connector.dataplane.store.sql.schema.DataPlaneStatements;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.time.Clock;

/**
 * SQL implementation of {@link DataPlaneStore}
 */
public class SqlDataPlaneStore extends AbstractSqlStore implements DataPlaneStore {


    private final DataPlaneStatements statements;

    private final Clock clock;

    public SqlDataPlaneStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                             DataPlaneStatements statements, ObjectMapper objectMapper, Clock clock, QueryExecutor queryExecutor) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
        this.clock = clock;
    }

    @Override
    public void received(String processId) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                upsert(connection, processId, State.RECEIVED);
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public void completed(String processId) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                upsert(connection, processId, State.COMPLETED);
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public State getState(String processId) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var state = stateById(connection, processId);
                return state != null ? state : State.NOT_TRACKED;
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });

    }

    public State mapToState(ResultSet resultSet) throws Exception {
        var stateCode = resultSet.getInt(statements.getStateColumn());
        return State.from(stateCode);
    }

    private void upsert(Connection connection, String processId, State state) {
        if (stateById(connection, processId) == null) {
            insert(connection, processId, state);
        } else {
            update(connection, processId, state);
        }
    }

    private State stateById(Connection connection, String processId) {
        var sql = statements.getFindByIdTemplate();
        return queryExecutor.single(connection, false, this::mapToState, sql, processId);
    }

    private void insert(Connection connection, String processId, State state) {
        var sql = statements.getInsertTemplate();
        var createdAt = clock.millis();
        queryExecutor.execute(connection, sql, processId, state.getCode(), createdAt, createdAt);
    }

    private void update(Connection connection, String processId, State state) {
        var sql = statements.getUpdateTemplate();
        var updatedAt = clock.millis();
        queryExecutor.execute(connection, sql, state.getCode(), updatedAt, processId);
    }
}
