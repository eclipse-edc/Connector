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

package org.eclipse.edc.edr.store.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.edr.store.index.sql.schema.EndpointDataReferenceEntryStatements;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class SqlEndpointDataReferenceEntryIndex extends AbstractSqlStore implements EndpointDataReferenceEntryIndex {

    private final EndpointDataReferenceEntryStatements statements;

    public SqlEndpointDataReferenceEntryIndex(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                              ObjectMapper objectMapper, EndpointDataReferenceEntryStatements statements, QueryExecutor queryExecutor) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public @Nullable EndpointDataReferenceEntry findById(String transferProcessId) {
        Objects.requireNonNull(transferProcessId);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findById(connection, transferProcessId);
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public StoreResult<List<EndpointDataReferenceEntry>> query(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            Objects.requireNonNull(querySpec);
            try {
                var queryStmt = statements.createQuery(querySpec);
                try (var stream = queryExecutor.query(getConnection(), true, this::mapResultSet, queryStmt.getQueryAsString(), queryStmt.getParameters())) {
                    return StoreResult.success(stream.collect(Collectors.toList()));
                }
            } catch (SQLException exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public StoreResult<Void> save(EndpointDataReferenceEntry entry) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (existsById(connection, entry.getTransferProcessId())) {
                    updateInternal(connection, entry);
                } else {
                    insertInternal(connection, entry);
                }
                return StoreResult.success();
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    @Override
    public StoreResult<EndpointDataReferenceEntry> delete(String transferProcessId) {
        Objects.requireNonNull(transferProcessId);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var entity = findById(connection, transferProcessId);
                if (entity != null) {
                    queryExecutor.execute(connection, statements.getDeleteByIdTemplate(), transferProcessId);
                    return StoreResult.success(entity);
                } else {
                    return StoreResult.notFound(format(ENDPOINT_DATA_REFERENCE_ENTRY_FOUND, transferProcessId));
                }
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    private EndpointDataReferenceEntry findById(Connection connection, String id) {
        var sql = statements.getFindByTemplate();
        return queryExecutor.single(connection, false, this::mapResultSet, sql, id);
    }

    private boolean existsById(Connection connection, String definitionId) {
        var sql = statements.getCountTemplate();
        try (var stream = queryExecutor.query(connection, false, this::mapCount, sql, definitionId)) {
            return stream.findFirst().orElse(0L) > 0;
        }
    }

    private long mapCount(ResultSet resultSet) throws SQLException {
        return resultSet.getLong(1);
    }

    private void insertInternal(Connection connection, EndpointDataReferenceEntry entry) {
        transactionContext.execute(() -> {
            queryExecutor.execute(connection, statements.getInsertTemplate(),
                    entry.getTransferProcessId(),
                    entry.getAssetId(),
                    entry.getProviderId(),
                    entry.getAgreementId(),
                    entry.getContractNegotiationId(),
                    entry.getCreatedAt(),
                    entry.getParticipantContextId());
        });
    }

    private void updateInternal(Connection connection, EndpointDataReferenceEntry entry) {
        transactionContext.execute(() -> {
            queryExecutor.execute(connection, statements.getUpdateTemplate(),
                    entry.getTransferProcessId(),
                    entry.getAssetId(),
                    entry.getProviderId(),
                    entry.getAgreementId(),
                    entry.getContractNegotiationId(),
                    entry.getCreatedAt(),
                    entry.getTransferProcessId());
        });
    }

    private EndpointDataReferenceEntry mapResultSet(ResultSet resultSet) throws Exception {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .assetId(resultSet.getString(statements.getAssetIdColumn()))
                .transferProcessId(resultSet.getString(statements.getTransferProcessIdColumn()))
                .agreementId(resultSet.getString(statements.getAgreementIdColumn()))
                .providerId(resultSet.getString(statements.getProviderIdColumn()))
                .contractNegotiationId(resultSet.getString(statements.getContractNegotiationIdColumn()))
                .participantContextId(resultSet.getString(statements.getParticipantContextIdColumn()))
                .build();
    }
}
