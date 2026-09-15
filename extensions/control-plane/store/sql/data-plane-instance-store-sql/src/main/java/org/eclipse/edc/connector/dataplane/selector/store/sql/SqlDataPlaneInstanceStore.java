/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.controlplane.dataplane.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.store.sql.schema.DataPlaneInstanceStatements;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * SQL store implementation of {@link DataPlaneInstanceStore}
 */
public class SqlDataPlaneInstanceStore extends AbstractSqlStore implements DataPlaneInstanceStore {

    private final DataPlaneInstanceStatements statements;

    public SqlDataPlaneInstanceStore(DataSourceRegistry dataSourceRegistry, String dataSourceName,
                                     TransactionContext transactionContext, DataPlaneInstanceStatements statements,
                                     ObjectMapper objectMapper, QueryExecutor queryExecutor) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public StoreResult<DataPlaneInstance> deleteById(String instanceId) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var instance = findById(instanceId);
                if (instance == null) {
                    return StoreResult.notFound("DataPlane instance %s not found".formatted(instanceId));
                }

                queryExecutor.execute(connection, statements.getDeleteByIdTemplate(), instanceId);

                return StoreResult.success(instance);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    @Override
    public DataPlaneInstance findById(String id) {
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findByIdInternal(connection, id);

            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }


    @Override
    public StoreResult<Void> save(DataPlaneInstance entity) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var sql = statements.getUpsertTemplate();
                queryExecutor.execute(connection, sql, entity.getId(), toJson(entity));
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public Stream<DataPlaneInstance> getAll() {
        return transactionContext.execute(() -> {
            try {
                var sql = statements.getAllTemplate();
                return queryExecutor.query(getConnection(), true, this::mapResultSet, sql);
            } catch (SQLException exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public Stream<DataPlaneInstance> query(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try {
                var statement = statements.createQuery(querySpec);
                return queryExecutor.query(getConnection(), true, this::mapResultSet, statement.getQueryAsString(), statement.getParameters());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private DataPlaneInstance findByIdInternal(Connection connection, String id) {
        var sql = statements.getFindByIdTemplate();
        return queryExecutor.single(connection, false, this::mapResultSet, sql, id);
    }

    private DataPlaneInstance mapResultSet(ResultSet resultSet) throws Exception {
        var json = resultSet.getString(statements.getDataColumn());
        return fromJson(json, DataPlaneInstance.class).copy();
    }

}
