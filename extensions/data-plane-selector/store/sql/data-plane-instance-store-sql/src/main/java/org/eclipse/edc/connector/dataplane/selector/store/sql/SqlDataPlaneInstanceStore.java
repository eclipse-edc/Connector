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
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.store.sql.schema.DataPlaneInstanceStatements;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.SqlLeaseContextBuilder;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * SQL store implementation of {@link DataPlaneInstanceStore}
 */
public class SqlDataPlaneInstanceStore extends AbstractSqlStore implements DataPlaneInstanceStore {

    private final DataPlaneInstanceStatements statements;
    private final SqlLeaseContextBuilder leaseContext;
    private final Clock clock;
    private final String leaseHolderName;

    public SqlDataPlaneInstanceStore(DataSourceRegistry dataSourceRegistry, String dataSourceName,
                                     TransactionContext transactionContext, DataPlaneInstanceStatements statements,
                                     ObjectMapper objectMapper, QueryExecutor queryExecutor, Clock clock, String leaseHolderName) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
        this.clock = clock;
        this.leaseHolderName = leaseHolderName;
        leaseContext = SqlLeaseContextBuilder.with(transactionContext, leaseHolderName, statements, clock, queryExecutor);
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
    public @NotNull List<DataPlaneInstance> nextNotLeased(int max, Criterion... criteria) {
        return transactionContext.execute(() -> {
            var filter = Arrays.stream(criteria).collect(toList());
            var querySpec = QuerySpec.Builder.newInstance().filter(filter).limit(max).build();
            var statement = statements.createQuery(querySpec)
                    .addWhereClause(statements.getNotLeasedFilter(), clock.millis());

            try (
                    var connection = getConnection();
                    var stream = queryExecutor.query(connection, true, this::mapResultSet, statement.getQueryAsString(), statement.getParameters())
            ) {
                var entries = stream.collect(Collectors.toList());
                entries.forEach(entry -> leaseContext.withConnection(connection).acquireLease(entry.getId()));
                return entries;
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<DataPlaneInstance> findByIdAndLease(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var entity = findByIdInternal(connection, id);
                if (entity == null) {
                    return StoreResult.notFound(format("DataPlaneInstance %s not found", id));
                }

                leaseContext.withConnection(connection).acquireLease(entity.getId());
                return StoreResult.success(entity);
            } catch (IllegalStateException e) {
                return StoreResult.alreadyLeased(format("DataPlaneInstance %s is already leased", id));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public void save(DataPlaneInstance entity) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var existing = findByIdInternal(connection, entity.getId());
                if (existing != null) {
                    leaseContext.by(leaseHolderName).withConnection(connection).breakLease(entity.getId());
                    update(connection, entity);
                } else {
                    insert(connection, entity);
                }
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

    private DataPlaneInstance findByIdInternal(Connection connection, String id) {
        var sql = statements.getFindByIdTemplate();
        return queryExecutor.single(connection, false, this::mapResultSet, sql, id);
    }

    private void insert(Connection connection, DataPlaneInstance instance) {
        var sql = statements.getInsertTemplate();
        queryExecutor.execute(connection, sql, instance.getId(), toJson(instance));
    }

    private void update(Connection connection, DataPlaneInstance instance) {
        var sql = statements.getUpdateTemplate();
        queryExecutor.execute(connection, sql, toJson(instance), instance.getId());
    }

    private DataPlaneInstance mapResultSet(ResultSet resultSet) throws Exception {
        var json = resultSet.getString(statements.getDataColumn());
        return fromJson(json, DataPlaneInstance.class).copy();
    }

}
