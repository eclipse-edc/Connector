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

package org.eclipse.edc.connector.policy.monitor.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntry;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.connector.policy.monitor.store.sql.schema.PolicyMonitorStatements;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilder;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.spi.query.Criterion.criterion;

public class SqlPolicyMonitorStore extends AbstractSqlStore implements PolicyMonitorStore {

    private final PolicyMonitorStatements statements;
    private final SqlLeaseContextBuilder leaseContext;
    private final String leaseHolderName;

    public SqlPolicyMonitorStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                 PolicyMonitorStatements statements, SqlLeaseContextBuilder leaseContext, ObjectMapper objectMapper,
                                 QueryExecutor queryExecutor, String leaseHolderName) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
        this.leaseHolderName = leaseHolderName;
        this.leaseContext = leaseContext;
    }

    @Override
    public @Nullable PolicyMonitorEntry findById(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findByIdInternal(connection, id);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @NotNull List<PolicyMonitorEntry> nextNotLeased(int max, Criterion... criteria) {
        return transactionContext.execute(() -> {
            var filter = Arrays.stream(criteria).collect(toList());
            var querySpec = QuerySpec.Builder.newInstance().filter(filter).sortField("stateTimestamp").limit(max).build();
            var statement = statements.createNextNotLeaseQuery(querySpec);
            try (
                    var connection = getConnection();
                    var stream = queryExecutor.query(connection, true, this::mapEntry, statement.getQueryAsString(), statement.getParameters())
            ) {
                return stream.filter(entry -> lease(connection, entry))
                        .collect(Collectors.toList());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private boolean lease(Connection connection, PolicyMonitorEntry entry) {
        return leaseContext.withConnection(connection).acquireLease(entry.getId()).succeeded();
    }

    @Override
    public StoreResult<PolicyMonitorEntry> findByIdAndLease(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var entity = findByIdInternal(connection, id);
                if (entity == null) {
                    return StoreResult.notFound(format("DataFlow %s not found", id));
                }
                return leaseContext.withConnection(connection).acquireLease(entity.getId()).map(v -> entity);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> save(PolicyMonitorEntry entity) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var sql = statements.getUpsertTemplate();

                queryExecutor.execute(connection, sql,
                        entity.getId(),
                        entity.getState(),
                        entity.getCreatedAt(),
                        entity.getUpdatedAt(),
                        entity.getStateCount(),
                        entity.getStateTimestamp(),
                        toJson(entity.getTraceContext()),
                        entity.getErrorDetail(),
                        entity.getContractId()
                );

                return leaseContext.by(leaseHolderName).withConnection(connection).breakLease(entity.getId());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private @Nullable PolicyMonitorEntry findByIdInternal(Connection conn, String id) {
        return transactionContext.execute(() -> {
            var querySpec = QuerySpec.Builder.newInstance().filter(criterion("id", "=", id)).build();
            var statement = statements.createQuery(querySpec);
            return queryExecutor.query(conn, true, this::mapEntry, statement.getQueryAsString(), statement.getParameters())
                    .findFirst().orElse(null);
        });
    }

    private PolicyMonitorEntry mapEntry(ResultSet resultSet) throws SQLException {
        return PolicyMonitorEntry.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .updatedAt(resultSet.getLong(statements.getUpdatedAtColumn()))
                .state(resultSet.getInt(statements.getStateColumn()))
                .stateTimestamp(resultSet.getLong(statements.getStateTimestampColumn()))
                .stateCount(resultSet.getInt(statements.getStateCountColumn()))
                .traceContext(fromJson(resultSet.getString(statements.getTraceContextColumn()), getTypeRef()))
                .errorDetail(resultSet.getString(statements.getErrorDetailColumn()))
                .contractId(resultSet.getString(statements.getContractIdColumn()))
                .build();
    }
}
