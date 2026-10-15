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

import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntry;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.connector.policy.monitor.store.sql.schema.PolicyMonitorStatements;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * SQL implementation of the {@link PolicyMonitorStore}.
 * The lease is stored directly on the policy monitor entry row, entries are fetched with {@code FOR UPDATE SKIP LOCKED}
 * so that concurrent runtimes never lease the same entry.
 */
public class SqlPolicyMonitorStore extends AbstractSqlStore implements PolicyMonitorStore {

    public static final Duration DEFAULT_LEASE_DURATION = Duration.ofSeconds(60);

    private final PolicyMonitorStatements statements;
    private final String leaseHolderName;
    private final Clock clock;
    private final Duration leaseDuration;

    public SqlPolicyMonitorStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                 PolicyMonitorStatements statements, Supplier<ObjectMapper> objectMapperSupplier,
                                 QueryExecutor queryExecutor, String leaseHolderName, Clock clock) {
        this(dataSourceRegistry, dataSourceName, transactionContext, statements, objectMapperSupplier, queryExecutor, leaseHolderName, clock, DEFAULT_LEASE_DURATION);
    }

    public SqlPolicyMonitorStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                 PolicyMonitorStatements statements, Supplier<ObjectMapper> objectMapperSupplier,
                                 QueryExecutor queryExecutor, String leaseHolderName, Clock clock, Duration leaseDuration) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapperSupplier, queryExecutor);
        this.statements = statements;
        this.leaseHolderName = leaseHolderName;
        this.clock = clock;
        this.leaseDuration = leaseDuration;
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
            var statement = statements.createNextNotLeaseQuery(querySpec, clock.millis());
            try (
                    var connection = getConnection();
                    var stream = queryExecutor.query(connection, false, this::mapEntry, statement.getQueryAsString(), statement.getParameters())
            ) {
                // rows are locked until the end of the transaction, so the lease can be safely acquired on them
                return stream.toList().stream()
                        .filter(entry -> acquireLease(connection, entry.getId()))
                        .toList();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<PolicyMonitorEntry> findByIdAndLease(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                // the lease is acquired before reading the entry, so that the returned entry cannot be outdated by a save
                // committed in between
                if (!acquireLease(connection, id)) {
                    return findByIdInternal(connection, id) == null
                            ? StoreResult.notFound(format("PolicyMonitorEntry %s not found", id))
                            : StoreResult.alreadyLeased(format("PolicyMonitorEntry %s is currently leased!", id));
                }
                return StoreResult.success(findByIdInternal(connection, id));
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

                // saving the entry breaks its lease, unless another holder owns it
                var updated = queryExecutor.execute(connection, sql,
                        entity.getId(),
                        entity.getState(),
                        entity.getCreatedAt(),
                        entity.getUpdatedAt(),
                        entity.getStateCount(),
                        entity.getStateTimestamp(),
                        toJson(entity.getTraceContext()),
                        entity.getErrorDetail(),
                        entity.getContractId(),
                        null,
                        null,
                        null,
                        // an existing entry is updated only if its lease is free, owned by this runtime or expired
                        leaseHolderName,
                        clock.millis()
                );

                if (updated == 0) {
                    return StoreResult.alreadyLeased(format("PolicyMonitorEntry %s is leased by another holder, cannot update it!", entity.getId()));
                }
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> breakLease(PolicyMonitorEntry entity) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var updated = queryExecutor.execute(connection, statements.getBreakLeaseTemplate(), entity.getId(), leaseHolderName, clock.millis());
                if (updated == 0 && findByIdInternal(connection, entity.getId()) != null) {
                    return StoreResult.alreadyLeased(format("Current runtime does not hold the lease for PolicyMonitorEntry %s, cannot break lease!", entity.getId()));
                }
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private boolean acquireLease(Connection connection, String id) {
        var now = clock.millis();
        return queryExecutor.execute(connection, statements.getAcquireLeaseTemplate(), leaseHolderName, now, leaseDuration.toMillis(), id, now) > 0;
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
