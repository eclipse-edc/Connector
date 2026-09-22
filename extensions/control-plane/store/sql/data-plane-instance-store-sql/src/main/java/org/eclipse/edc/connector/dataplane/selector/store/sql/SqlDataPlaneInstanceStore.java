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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.AuthorizationProfile;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * SQL store implementation of {@link DataPlaneInstanceStore}: every field of the {@link DataPlaneInstance} is stored in
 * its own column, collections and maps are serialized as JSON.
 */
public class SqlDataPlaneInstanceStore extends AbstractSqlStore implements DataPlaneInstanceStore {

    private static final TypeReference<Set<String>> SET_OF_STRINGS = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_OF_OBJECTS = new TypeReference<>() {
    };

    private final DataPlaneInstanceStatements statements;

    public SqlDataPlaneInstanceStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                     DataPlaneInstanceStatements statements, ObjectMapper objectMapper, QueryExecutor queryExecutor) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public StoreResult<DataPlaneInstance> deleteById(String instanceId) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var instance = findByIdInternal(connection, instanceId);
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
    @SuppressWarnings("deprecation")
    public StoreResult<Void> save(DataPlaneInstance entity) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var sql = statements.getUpsertTemplate();
                queryExecutor.execute(connection, sql,
                        entity.getId(),
                        entity.getState(),
                        entity.getStateTimestamp(),
                        entity.getCreatedAt(),
                        entity.getUpdatedAt(),
                        entity.getUrl().toString(),
                        entity.getLastActive(),
                        entity.getParticipantContextId(),
                        toJson(entity.getAllowedSourceTypes()),
                        toJson(entity.getAllowedTransferTypes()),
                        toJson(entity.getDestinationProvisionTypes()),
                        toJson(entity.getLabels()),
                        toJson(entity.getProperties()),
                        toJson(entity.getAuthorizationProfile())
                );
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

    @SuppressWarnings("deprecation")
    private DataPlaneInstance mapResultSet(ResultSet resultSet) throws Exception {
        return DataPlaneInstance.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .state(resultSet.getInt(statements.getStateColumn()))
                .stateTimestamp(resultSet.getLong(statements.getStateTimestampColumn()))
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .updatedAt(resultSet.getLong(statements.getUpdatedAtColumn()))
                .url(resultSet.getString(statements.getUrlColumn()))
                .lastActive(resultSet.getLong(statements.getLastActiveColumn()))
                .participantContextId(resultSet.getString(statements.getParticipantContextIdColumn()))
                .allowedSourceTypes(fromJson(resultSet.getString(statements.getAllowedSourceTypesColumn()), SET_OF_STRINGS))
                .allowedTransferType(fromJson(resultSet.getString(statements.getAllowedTransferTypesColumn()), SET_OF_STRINGS))
                .destinationProvisionTypes(fromJson(resultSet.getString(statements.getDestinationProvisionTypesColumn()), SET_OF_STRINGS))
                .labels(fromJson(resultSet.getString(statements.getLabelsColumn()), SET_OF_STRINGS))
                .properties(fromJson(resultSet.getString(statements.getPropertiesColumn()), MAP_OF_OBJECTS))
                .authorizationProfile(fromJson(resultSet.getString(statements.getAuthorizationProfileColumn()), AuthorizationProfile.class))
                .build();
    }
}
