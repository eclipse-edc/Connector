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
 *       Fraunhofer Institute for Software and Systems Engineering - added method
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema.ContractDefinitionStatements;
import org.eclipse.dataspaceconnector.sql.store.AbstractSqlStore;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuerySingle;

public class SqlContractDefinitionStore extends AbstractSqlStore implements ContractDefinitionStore {

    private final ContractDefinitionStatements statements;

    public SqlContractDefinitionStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext, ContractDefinitionStatements statements, ObjectMapper objectMapper) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper);
        this.statements = statements;
    }

    @Override
    public @NotNull Stream<ContractDefinition> findAll(QuerySpec spec) {
        return transactionContext.execute(() -> {
            Objects.requireNonNull(spec);

            try {
                var queryStmt = statements.createQuery(spec);
                return executeQuery(getConnection(), true, this::mapResultSet, queryStmt.getQueryAsString(), queryStmt.getParameters());
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
    public void save(Collection<ContractDefinition> definitions) {
        Objects.requireNonNull(definitions);

        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                for (var definition : definitions) {
                    if (existsById(connection, definition.getId())) {
                        updateInternal(connection, definition);
                    } else {
                        insertInternal(connection, definition);
                    }
                }
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    @Override
    public void save(ContractDefinition definition) {
        save(Collections.singletonList(definition));
    }

    @Override
    public void update(ContractDefinition definition) {
        save(definition); //upsert
    }

    @Override
    public ContractDefinition deleteById(String id) {
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var entity = findById(connection, id);
                if (entity != null) {
                    executeQuery(connection, statements.getDeleteByIdTemplate(), id);
                }
                return entity;
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
                .selectorExpression(fromJson(resultSet.getString(statements.getSelectorExpressionColumn()), AssetSelectorExpression.class))
                .build();
    }

    private void insertInternal(Connection connection, ContractDefinition definition) {
        transactionContext.execute(() -> {
            executeQuery(connection, statements.getInsertTemplate(),
                    definition.getId(),
                    definition.getAccessPolicyId(),
                    definition.getContractPolicyId(),
                    toJson(definition.getSelectorExpression()),
                    definition.getCreatedAt());
        });
    }

    private void updateInternal(Connection connection, ContractDefinition definition) {
        Objects.requireNonNull(definition);
        transactionContext.execute(() -> {
            if (!existsById(connection, definition.getId())) {
                throw new EdcPersistenceException(format("Cannot update. Contract Definition with ID '%s' does not exist.", definition.getId()));
            }

            executeQuery(connection, statements.getUpdateTemplate(),
                    definition.getId(),
                    definition.getAccessPolicyId(),
                    definition.getContractPolicyId(),
                    toJson(definition.getSelectorExpression()),
                    definition.getCreatedAt(),
                    definition.getId());

        });
    }


    private boolean existsById(Connection connection, String definitionId) {
        var sql = statements.getCountTemplate();
        try (var stream = executeQuery(connection, false, this::mapCount, sql, definitionId)) {
            return stream.findFirst().orElse(0L) > 0;
        }
    }

    private long mapCount(ResultSet resultSet) throws SQLException {
        return resultSet.getLong(1);
    }

    private ContractDefinition findById(Connection connection, String id) {
        var sql = statements.getFindByTemplate();
        return executeQuerySingle(connection, false, this::mapResultSet, sql, id);
    }

}
