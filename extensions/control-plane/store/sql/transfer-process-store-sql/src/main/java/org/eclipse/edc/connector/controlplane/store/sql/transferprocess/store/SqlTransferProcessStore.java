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
 *       Mercedes-Benz Tech Innovation GmbH - connector id removal
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.TransferProcessStoreStatements;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedResourceSet;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.entity.ProtocolMessages;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * Implementation of the {@link TransferProcessStore} based on SQL.
 */
public class SqlTransferProcessStore extends AbstractSqlStore implements TransferProcessStore {
    private final TransferProcessStoreStatements statements;
    private final SqlLeaseContextBuilder leaseContext;

    public SqlTransferProcessStore(DataSourceRegistry dataSourceRegistry, String datasourceName,
                                   TransactionContext transactionContext, ObjectMapper objectMapper,
                                   TransferProcessStoreStatements statements, SqlLeaseContextBuilder leaseContext,
                                   QueryExecutor queryExecutor) {
        super(dataSourceRegistry, datasourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
        this.leaseContext = leaseContext;
    }

    @Override
    public @Nullable TransferProcess findById(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findByIdInternal(connection, id);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @NotNull List<TransferProcess> nextNotLeased(int max, Criterion... criteria) {
        return transactionContext.execute(() -> {
            var filter = Arrays.stream(criteria).collect(toList());
            var querySpec = QuerySpec.Builder.newInstance().filter(filter).sortField("stateTimestamp").limit(max).build();
            var statement = statements.createNextNotLeaseQuery(querySpec);
            try (
                    var connection = getConnection();
                    var stream = queryExecutor.query(connection, true, this::mapTransferProcess, statement.getQueryAsString(), statement.getParameters())
            ) {
                return stream.filter(tp -> lease(connection, tp)).collect(Collectors.toList());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private boolean lease(Connection connection, TransferProcess entry) {
        return leaseContext.withConnection(connection).acquireLease(entry.getId()).succeeded();
    }

    @Override
    public StoreResult<TransferProcess> findByIdAndLease(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var entity = findByIdInternal(connection, id);
                if (entity == null) {
                    return StoreResult.notFound(format("TransferProcess %s not found", id));
                }

                return leaseContext.withConnection(connection).acquireLease(entity.getId()).map(it -> entity);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> save(TransferProcess entity) {
        Objects.requireNonNull(entity.getId(), "TransferProcesses must have an ID!");
        return transactionContext.execute(() -> {
            try (var conn = getConnection()) {
                var sql = statements.getUpsertStatement();
                queryExecutor.execute(conn, sql, entity.getId(),
                        entity.getState(),
                        entity.getStateCount(),
                        entity.getStateTimestamp(),
                        entity.getCreatedAt(),
                        entity.getUpdatedAt(),
                        toJson(entity.getTraceContext()),
                        entity.getErrorDetail(),
                        toJson(entity.getResourceManifest()),
                        toJson(entity.getProvisionedResourceSet()),
                        toJson(entity.getContentDataAddress()),
                        entity.getType().toString(),
                        toJson(entity.getDeprovisionedResources()),
                        toJson(entity.getPrivateProperties()),
                        toJson(entity.getCallbackAddresses()),
                        entity.isPending(),
                        entity.getTransferType(),
                        toJson(entity.getProtocolMessages()),
                        entity.getDataPlaneId(),
                        entity.getCorrelationId(),
                        entity.getCounterPartyAddress(),
                        entity.getProtocol(),
                        entity.getAssetId(),
                        entity.getContractId(),
                        toJson(entity.getDataDestination()),
                        entity.getParticipantContextId(),
                        toJson(entity.getDataplaneMetadata()));

                return leaseContext.withConnection(conn).breakLease(entity.getId());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @Nullable TransferProcess findForCorrelationId(String correlationId) {
        return transactionContext.execute(() -> {
            var query = correlationIdQuerySpec(correlationId);
            try (var stream = findAll(query)) {
                return single(stream.collect(toList()));
            }
        });
    }

    @Override
    public StoreResult<Void> delete(String processId) {
        return transactionContext.execute(() -> {
            var existing = findById(processId);
            if (existing != null) {
                try (var conn = getConnection()) {
                    // attempt to acquire lease - should fail if someone else holds the lease
                    var result = leaseContext.withConnection(conn).acquireLease(processId);
                    if (result.failed()) {
                        return result;
                    }

                    var stmt = statements.getDeleteTransferProcessTemplate();
                    queryExecutor.execute(conn, stmt, processId);

                    //necessary to delete the row in edc_lease
                    return leaseContext.withConnection(conn).breakLease(processId);
                } catch (SQLException e) {
                    throw new EdcPersistenceException(e);
                }
            }
            return StoreResult.success();
        });
    }

    @Override
    public Stream<TransferProcess> findAll(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try (var conn = getConnection()) {
                return executeQuery(conn, querySpec);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private QuerySpec correlationIdQuerySpec(String correlationId) {
        var criterion = criterion("correlationId", "=", correlationId);
        return QuerySpec.Builder.newInstance().filter(criterion).build();
    }

    private @Nullable TransferProcess findByIdInternal(Connection conn, String id) {
        return transactionContext.execute(() -> {
            var querySpec = QuerySpec.Builder.newInstance().filter(criterion("id", "=", id)).build();
            return single(executeQuery(conn, querySpec).collect(toList()));
        });
    }

    private Stream<TransferProcess> executeQuery(Connection connection, QuerySpec querySpec) {
        var statement = statements.createQuery(querySpec);
        return queryExecutor.query(connection, true, this::mapTransferProcess, statement.getQueryAsString(), statement.getParameters());
    }

    /**
     * Returns either a single element from the list, or null if empty. Throws an IllegalStateException if the list has
     * more than 1 element
     */
    @Nullable
    private <T> T single(List<T> list) {
        if (list.size() > 1) {
            throw new IllegalStateException(getMultiplicityError(1, list.size()));
        }
        return list.isEmpty() ? null : list.get(0);
    }

    private String getMultiplicityError(int expectedSize, int actualSize) {
        return format("Expected to find %d items, but found %d", expectedSize, actualSize);
    }

    private TransferProcess mapTransferProcess(ResultSet resultSet) throws SQLException {
        return TransferProcess.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .type(TransferProcess.Type.valueOf(resultSet.getString(statements.getTypeColumn())))
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .updatedAt(resultSet.getLong(statements.getUpdatedAtColumn()))
                .state(resultSet.getInt(statements.getStateColumn()))
                .stateTimestamp(resultSet.getLong(statements.getStateTimestampColumn()))
                .stateCount(resultSet.getInt(statements.getStateCountColumn()))
                .traceContext(fromJson(resultSet.getString(statements.getTraceContextColumn()), getTypeRef()))
                .resourceManifest(fromJson(resultSet.getString(statements.getResourceManifestColumn()), ResourceManifest.class))
                .provisionedResourceSet(fromJson(resultSet.getString(statements.getProvisionedResourceSetColumn()), ProvisionedResourceSet.class))
                .errorDetail(resultSet.getString(statements.getErrorDetailColumn()))
                .correlationId(resultSet.getString(statements.getCorrelationIdColumn()))
                .assetId(resultSet.getString(statements.getAssetIdColumn()))
                .protocol(resultSet.getString(statements.getProtocolColumn()))
                .dataDestination(fromJson(resultSet.getString(statements.getDataDestinationColumn()), DataAddress.class))
                .counterPartyAddress(resultSet.getString(statements.getCounterPartyAddressColumn()))
                .contractId(resultSet.getString(statements.getContractIdColumn()))
                .contentDataAddress(fromJson(resultSet.getString(statements.getContentDataAddressColumn()), DataAddress.class))
                .deprovisionedResources(fromJson(resultSet.getString(statements.getDeprovisionedResourcesColumn()), new TypeReference<>() {
                }))
                .callbackAddresses(fromJson(resultSet.getString(statements.getCallbackAddressesColumn()), new TypeReference<>() {
                }))
                .privateProperties(fromJson(resultSet.getString(statements.getPrivatePropertiesColumn()), getTypeRef()))
                .pending(resultSet.getBoolean(statements.getPendingColumn()))
                .transferType(resultSet.getString(statements.getTransferTypeColumn()))
                .protocolMessages(fromJson(resultSet.getString(statements.getProtocolMessagesColumn()), ProtocolMessages.class))
                .dataPlaneId(resultSet.getString(statements.getDataPlaneIdColumn()))
                .participantContextId(resultSet.getString(statements.getParticipantContextIdColumn()))
                .dataplaneMetadata(fromJson(resultSet.getString(statements.getDataplaneMetadata()), DataplaneMetadata.class))
                .build();
    }

}
