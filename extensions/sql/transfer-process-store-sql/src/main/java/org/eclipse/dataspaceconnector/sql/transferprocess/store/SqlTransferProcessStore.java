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

package org.eclipse.dataspaceconnector.sql.transferprocess.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResourceSet;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.eclipse.dataspaceconnector.sql.lease.SqlLeaseContextBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.sql.DataSource;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

public class SqlTransferProcessStore implements TransferProcessStore {
    private final DataSourceRegistry dataSourceRegistry;
    private final String datasourceName;
    private final TransactionContext transactionContext;
    private final ObjectMapper objectMapper;
    private final TransferProcessStoreStatements statements;
    private final String leaseHolderName;
    private final SqlLeaseContextBuilder leaseContext;
    private final Clock clock;

    public SqlTransferProcessStore(DataSourceRegistry dataSourceRegistry, String datasourceName, TransactionContext transactionContext, ObjectMapper objectMapper, TransferProcessStoreStatements statements, String leaseHolderName, Clock clock) {
        this.dataSourceRegistry = dataSourceRegistry;
        this.datasourceName = datasourceName;
        this.transactionContext = transactionContext;
        this.objectMapper = objectMapper;
        this.statements = statements;
        this.leaseHolderName = leaseHolderName;
        this.clock = clock;
        leaseContext = SqlLeaseContextBuilder.with(transactionContext, leaseHolderName, statements, clock);
    }

    @Override
    public @NotNull List<TransferProcess> nextForState(int state, int max) {
        var list = new ArrayList<TransferProcess>();
        var now = clock.millis();
        transactionContext.execute(() -> {
            try (var conn = getConnection()) {
                var stmt = statements.getNextForStateTemplate();

                var tmpResult = executeQuery(conn, this::mapTransferProcess, stmt, state, now, max);
                list.addAll(tmpResult);
                list.forEach(t -> leaseContext.by(leaseHolderName).withConnection(conn).acquireLease(t.getId()));

            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });

        return list;
    }

    @Override
    public @Nullable TransferProcess find(String id) {
        return transactionContext.execute(() -> {
            try (var conn = getConnection()) {
                var stmt = statements.getFindByIdStatement();

                return single(executeQuery(conn, this::mapTransferProcess, stmt, id));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @Nullable String processIdForTransferId(String transferId) {
        return transactionContext.execute(() -> {
            try (var conn = getConnection()) {
                var stmt = statements.getProcessIdForTransferIdTemplate();
                return single(executeQuery(conn, (rs) -> rs.getString(statements.getIdColumn()), stmt, transferId));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public void create(TransferProcess process) {
        transactionContext.execute(() -> {
            if (find(process.getId()) != null) {
                update(process);
            } else {
                insert(process);
            }
        });

    }

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
                    update(conn, id, process);
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
            try (var conn = getConnection()) {
                var stmt = statements.getQueryStatement();
                //todo: add filtering, sorting
                return executeQuery(conn, this::mapTransferProcess, stmt, querySpec.getLimit(), querySpec.getOffset()).stream();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private void update(Connection conn, String transferProcessId, TransferProcess process) {
        var stmt = statements.getUpdateTransferProcessTemplate();
        executeQuery(conn, stmt, process.getState(),
                process.getStateCount(),
                process.getStateTimestamp(),
                toJson(process.getTraceContext()),
                process.getErrorDetail(),
                toJson(process.getResourceManifest()),
                toJson(process.getProvisionedResourceSet()),
                toJson(process.getContentDataAddress()),
                transferProcessId);
    }

    /**
     * Returns either a single element from the list, or null if empty.
     * Throws an IllegalStateException if the list has more than 1 element
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
                        process.getCreatedTimestamp(),
                        toJson(process.getTraceContext()),
                        process.getErrorDetail(),
                        toJson(process.getResourceManifest()),
                        toJson(process.getProvisionedResourceSet()),
                        toJson(process.getContentDataAddress()),
                        process.getType().toString());

                //insert DataRequest
                var dr = process.getDataRequest();
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
                        process.getId(),
                        dr.getProtocol(),
                        dr.isManagedResources());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private TransferProcess mapTransferProcess(ResultSet resultSet) throws SQLException {
        return TransferProcess.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .type(TransferProcess.Type.valueOf(resultSet.getString(statements.getTypeColumn())))
                .createdTimestamp(resultSet.getLong(statements.getCreatedTimestampColumn()))
                .state(resultSet.getInt(statements.getStateColumn()))
                .stateTimestamp(resultSet.getLong(statements.getStateTimestampColumn()))
                .stateCount(resultSet.getInt(statements.getStateCountColumn()))
                .traceContext(fromJson(resultSet.getString(statements.getTraceContextColumn()), getTypeRef()))
                .resourceManifest(fromJson(resultSet.getString(statements.getResourceManifestColumn()), ResourceManifest.class))
                .provisionedResourceSet(fromJson(resultSet.getString(statements.getProvisionedResourcesetColumn()), ProvisionedResourceSet.class))
                .errorDetail(resultSet.getString(statements.getErrorDetailColumn()))
                .dataRequest(extractDataRequest(resultSet))
                .contentDataAddress(fromJson(resultSet.getString(statements.getContentDataAddressColumn()), DataAddress.class))
                .build();
    }

    private DataRequest extractDataRequest(ResultSet resultSet) throws SQLException {
        return mapDataRequest(resultSet);
    }

    private DataRequest mapDataRequest(ResultSet resultSet) throws SQLException {
        return DataRequest.Builder.newInstance()
                .id(resultSet.getString("edc_data_request_id"))
                .assetId(resultSet.getString(statements.getAssetIdColumn()))
                .protocol(resultSet.getString(statements.getProtocolColumn()))
                .dataDestination(fromJson(resultSet.getString(statements.getDestinationColumn()), DataAddress.class))
                .connectorId(resultSet.getString(statements.getConnectorIdColumn()))
                .connectorAddress(resultSet.getString(statements.getConnectorAddressColumn()))
                .contractId(resultSet.getString(statements.getContractIdColumn()))
                .managedResources(resultSet.getBoolean(statements.getManagedResourcesColumn()))
                .transferType(fromJson(resultSet.getString(statements.getTransferTypeColumn()), TransferType.class))
                .processId(resultSet.getString(statements.getProcessIdColumn()))
                .properties(fromJson(resultSet.getString(statements.getPropertiesColumn()), getTypeRef()))
                .build();
    }

    @NotNull
    private <T> TypeReference<T> getTypeRef() {
        return new TypeReference<>() {
        };
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
    }

    private <T> T fromJson(String json, Class<T> typeRef) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
    }

    private Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    private DataSource getDataSource() {
        return dataSourceRegistry.resolve(datasourceName);
    }
}
