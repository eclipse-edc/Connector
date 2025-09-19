/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Microsoft Corporation - refactoring, bugfixing
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - added method
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *       SAP SE - add private properties to contract definition
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.contractdefinition;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.store.sql.contractdefinition.schema.ContractDefinitionStatements;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;

public class SqlContractDefinitionStore extends AbstractSqlStore implements ContractDefinitionStore {

    public static final TypeReference<List<Criterion>> CRITERION_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> PRIVATE_PROPERTIES_TYPE = new TypeReference<>() {
    };
    private final ContractDefinitionStatements statements;

    public SqlContractDefinitionStore(DataSourceRegistry dataSourceRegistry, String dataSourceName,
                                      TransactionContext transactionContext, ContractDefinitionStatements statements,
                                      ObjectMapper objectMapper, QueryExecutor queryExecutor) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public @NotNull Stream<ContractDefinition> findAll(QuerySpec spec) {
        return transactionContext.execute(() -> {
            Objects.requireNonNull(spec);

            try {
                var queryStmt = statements.createQuery(spec);
                return queryExecutor.query(getConnection(), true, this::mapResultSet, queryStmt.getQueryAsString(), queryStmt.getParameters());
            } catch (SQLException exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public ContractDefinition findById(String definitionId) {
        Objects.requireNonNull(definitionId);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findById(connection, definitionId);
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }


    @Override
    public StoreResult<Void> save(ContractDefinition definition) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (existsById(connection, definition.getId())) {
                    return StoreResult.alreadyExists(format(CONTRACT_DEFINITION_EXISTS, definition.getId()));
                } else {
                    insertInternal(connection, definition);
                    return StoreResult.success();

                }
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    @Override
    public StoreResult<Void> update(ContractDefinition definition) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (existsById(connection, definition.getId())) {
                    updateInternal(connection, definition);
                    return StoreResult.success();
                } else {
                    return StoreResult.notFound(format(CONTRACT_DEFINITION_NOT_FOUND, definition.getId()));
                }
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    @Override
    public StoreResult<ContractDefinition> deleteById(String id) {
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var entity = findById(connection, id);
                if (entity != null) {
                    queryExecutor.execute(connection, statements.getDeleteByIdTemplate(), id);
                    return StoreResult.success(entity);
                } else {
                    return StoreResult.notFound(format(CONTRACT_DEFINITION_NOT_FOUND, id));
                }

            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });

    }

    private ContractDefinition mapResultSet(ResultSet resultSet) throws Exception {
        return ContractDefinition.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .accessPolicyId(resultSet.getString(statements.getAccessPolicyIdColumn()))
                .contractPolicyId(resultSet.getString(statements.getContractPolicyIdColumn()))
                .assetsSelector(fromJson(resultSet.getString(statements.getAssetsSelectorColumn()), CRITERION_LIST))
                .privateProperties(fromJson(resultSet.getString(statements.getPrivatePropertiesColumn()), PRIVATE_PROPERTIES_TYPE))
                .participantContextId(resultSet.getString(statements.getParticipantContextIdColumn()))
                .build();
    }

    private void insertInternal(Connection connection, ContractDefinition definition) {
        transactionContext.execute(() -> {
            queryExecutor.execute(connection, statements.getInsertTemplate(),
                    definition.getId(),
                    definition.getAccessPolicyId(),
                    definition.getContractPolicyId(),
                    toJson(definition.getAssetsSelector()),
                    definition.getCreatedAt(),
                    toJson(definition.getPrivateProperties()),
                    definition.getParticipantContextId());
        });
    }

    private void updateInternal(Connection connection, ContractDefinition definition) {
        Objects.requireNonNull(definition);
        queryExecutor.execute(connection, statements.getUpdateTemplate(),
                definition.getId(),
                definition.getAccessPolicyId(),
                definition.getContractPolicyId(),
                toJson(definition.getAssetsSelector()),
                definition.getCreatedAt(),
                toJson(definition.getPrivateProperties()),
                definition.getId());
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

    private ContractDefinition findById(Connection connection, String id) {
        var sql = statements.getFindByTemplate();
        return queryExecutor.single(connection, false, this::mapResultSet, sql, id);
    }
}
