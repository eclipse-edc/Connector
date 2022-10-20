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

package org.eclipse.edc.connector.store.sql.transferprocess.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.store.sql.transferprocess.store.schema.TransferProcessStoreStatements;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResourceSet;
import org.eclipse.edc.connector.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferType;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.sql.lease.SqlLeaseContextBuilder;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuerySingle;

/**
 * Implementation of the {@link TransferProcessStore} based on SQL.
 */
public class SqlTransferProcessStore extends AbstractSqlStore implements TransferProcessStore {
    private final TransferProcessStoreStatements statements;
    private final String leaseHolderName;
    private final SqlLeaseContextBuilder leaseContext;
    private final Clock clock;

    public SqlTransferProcessStore(DataSourceRegistry dataSourceRegistry, String datasourceName, TransactionContext transactionContext, ObjectMapper objectMapper, TransferProcessStoreStatements statements, String leaseHolderName, Clock clock) {
        super(dataSourceRegistry, datasourceName, transactionContext, objectMapper);
        this.statements = statements;
        this.leaseHolderName = leaseHolderName;
        this.clock = clock;
        leaseContext = SqlLeaseContextBuilder.with(transactionContext, leaseHolderName, statements, clock);
    }

    @Override
    public @NotNull List<TransferProcess> nextForState(int state, int max) {
        var now = clock.millis();
        return transactionContext.execute(() -> {
            var stmt = statements.getNextForStateTemplate();

            try (
                    var connection = getConnection();
                    var stream = executeQuery(connection, true, this::mapTransferProcess, stmt, state, now, max)
            ) {
                var transferProcesses = stream.collect(toList());
                transferProcesses.forEach(t -> leaseContext.by(leaseHolderName).withConnection(connection).acquireLease(t.getId()));
                return transferProcesses;

            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @Nullable TransferProcess find(String id) {
        return transactionContext.execute(() -> {
            var q = QuerySpec.Builder.newInstance().filter("id = " + id).build();
            return single(findAll(q).collect(toList()));
        });
    }

    @Override
    public @Nullable String processIdForDataRequestId(String transferId) {
        return transactionContext.execute(() -> {
            var stmt = statements.getProcessIdForTransferIdTemplate();
            try {
                return executeQuerySingle(getConnection(), true, (rs) -> rs.getString(statements.getIdColumn()), stmt, transferId);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    /**
     * Creates a new {@link TransferProcess}, or updates if one already exists.
     *
     * @param process The new TransferProcess.
     * @throws IllegalArgumentException if the TransferProcess does not have a {@link DataRequest}.
     */
    @Override
    public void create(TransferProcess process) {
        if (process.getDataRequest() == null) {
            throw new IllegalArgumentException("Cannot store TransferProcess without a DataRequest");
        }
        transactionContext.execute(() -> {
            if (find(process.getId()) != null) {
                update(process);
            } else {
                insert(process);
            }
        });

    }

    /**
     * Updates a TransferProcess overwriting all properties. The {@link DataRequest} that is associated with the {@link TransferProcess}
     * will get updated including its ID (primary key).
     *
     * @param process The new TransferProcess
     */
    @Override
    public void update(TransferProcess process) {
        transactionContext.execute(() -> {
            String id = process.getId();
            var existing = find(id);

            if (existing == null) {
                insert(process);
            } else {
                try (var conn = getConnection()) {
                    leaseContext.by(leaseHolderName).withConnection(conn).breakLease(id);
                    update(conn, process, existing.getDataRequest().getId());
                } catch (SQLException e) {
                    throw new EdcPersistenceException(e);
                }
            }
        });
    }

    @Override
    public void delete(String processId) {

        transactionContext.execute(() -> {
            var existing = find(processId);
            if (existing != null) {
                try (var conn = getConnection()) {
                    // attempt to acquire lease - should fail if someone else holds the lease
                    leaseContext.by(leaseHolderName).withConnection(conn).acquireLease(processId);

                    var stmt = statements.getDeleteTransferProcessTemplate();
                    executeQuery(conn, stmt, processId);

                    //necessary to delete the row in edc_lease
                    leaseContext.by(leaseHolderName).withConnection(conn).breakLease(processId);
                } catch (SQLException e) {
                    throw new EdcPersistenceException(e);
                }
            }

        });
    }

    @Override
    public Stream<TransferProcess> findAll(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try {
                var statement = statements.createQuery(querySpec);
                return executeQuery(getConnection(), true, this::mapTransferProcess, statement.getQueryAsString(), statement.getParameters());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    public DataRequest mapDataRequest(ResultSet resultSet) throws SQLException {
        return DataRequest.Builder.newInstance()
                .id(resultSet.getString("edc_data_request_id"))
                .assetId(resultSet.getString(statements.getAssetIdColumn()))
                .protocol(resultSet.getString(statements.getProtocolColumn()))
                .dataDestination(fromJson(resultSet.getString(statements.getDataDestinationColumn()), DataAddress.class))
                .connectorId(resultSet.getString(statements.getConnectorIdColumn()))
                .connectorAddress(resultSet.getString(statements.getConnectorAddressColumn()))
                .contractId(resultSet.getString(statements.getContractIdColumn()))
                .managedResources(resultSet.getBoolean(statements.getManagedResourcesColumn()))
                .transferType(fromJson(resultSet.getString(statements.getTransferTypeColumn()), TransferType.class))
                .processId(resultSet.getString(statements.getProcessIdColumn()))
                .properties(fromJson(resultSet.getString(statements.getPropertiesColumn()), getTypeRef()))
                .build();
    }

    private void update(Connection conn, TransferProcess process, String existingDataRequestId) {
        var updateStmt = statements.getUpdateTransferProcessTemplate();
        executeQuery(conn, updateStmt, process.getState(),
                process.getStateCount(),
                process.getStateTimestamp(),
                toJson(process.getTraceContext()),
                process.getErrorDetail(),
                toJson(process.getResourceManifest()),
                toJson(process.getProvisionedResourceSet()),
                toJson(process.getContentDataAddress()),
                toJson(process.getDeprovisionedResources()),
                process.getUpdatedAt(),
                process.getId());

        var newDr = process.getDataRequest();
        updateDataRequest(conn, newDr, existingDataRequestId);
    }

    private void updateDataRequest(Connection conn, DataRequest dataRequest, String existingDataRequestId) {
        var updateDrStmt = statements.getUpdateDataRequestTemplate();

        executeQuery(conn, updateDrStmt,
                dataRequest.getId(),
                dataRequest.getProcessId(),
                dataRequest.getConnectorAddress(),
                dataRequest.getProtocol(),
                dataRequest.getConnectorId(),
                dataRequest.getAssetId(),
                dataRequest.getContractId(),
                toJson(dataRequest.getDataDestination()),
                dataRequest.isManagedResources(),
                toJson(dataRequest.getProperties()),
                toJson(dataRequest.getTransferType()),
                existingDataRequestId);
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

    private void insert(TransferProcess process) {
        transactionContext.execute(() -> {
            try (var conn = getConnection()) {
                // insert TransferProcess
                var insertTpStatement = statements.getInsertStatement();

                executeQuery(conn, insertTpStatement, process.getId(),
                        process.getState(),
                        process.getStateCount(),
                        process.getStateTimestamp(),
                        process.getCreatedAt(),
                        process.getUpdatedAt(),
                        toJson(process.getTraceContext()),
                        process.getErrorDetail(),
                        toJson(process.getResourceManifest()),
                        toJson(process.getProvisionedResourceSet()),
                        toJson(process.getContentDataAddress()),
                        process.getType().toString(),
                        toJson(process.getDeprovisionedResources()));

                //insert DataRequest
                var dr = process.getDataRequest();
                if (dr != null) {
                    insertDataRequest(process.getId(), dr, conn);
                }
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private void insertDataRequest(String processId, DataRequest dr, Connection conn) {
        var insertDrStmt = statements.getInsertDataRequestTemplate();
        executeQuery(conn, insertDrStmt,
                dr.getId(),
                dr.getProcessId(),
                dr.getConnectorAddress(),
                dr.getConnectorId(),
                dr.getAssetId(),
                dr.getContractId(),
                toJson(dr.getDataDestination()),
                toJson(dr.getProperties()),
                toJson(dr.getTransferType()),
                processId,
                dr.getProtocol(),
                dr.isManagedResources());
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
                .provisionedResourceSet(fromJson(resultSet.getString(statements.getProvisionedResourcesetColumn()), ProvisionedResourceSet.class))
                .errorDetail(resultSet.getString(statements.getErrorDetailColumn()))
                .dataRequest(mapDataRequest(resultSet))
                .contentDataAddress(fromJson(resultSet.getString(statements.getContentDataAddressColumn()), DataAddress.class))
                .deprovisionedResources(fromJson(resultSet.getString(statements.getDeprovisionedResourcesColumn()), new TypeReference<>() {
                }))
                .build();
    }

    @NotNull
    private <T> TypeReference<T> getTypeRef() {
        return new TypeReference<>() {
        };
    }

}
