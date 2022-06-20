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


import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema.ContractDefinitionStatements;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

public class SqlContractDefinitionStore implements ContractDefinitionStore {

    private final TypeManager typeManager;
    private final DataSourceRegistry dataSourceRegistry;
    private final String dataSourceName;
    private final TransactionContext transactionContext;
    private final ContractDefinitionStatements statements;

    public SqlContractDefinitionStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext, ContractDefinitionStatements statements, TypeManager typeManager) {
        this.dataSourceRegistry = Objects.requireNonNull(dataSourceRegistry);
        this.dataSourceName = Objects.requireNonNull(dataSourceName);
        this.transactionContext = Objects.requireNonNull(transactionContext);
        this.statements = statements;
        this.typeManager = Objects.requireNonNull(typeManager);
    }

    ContractDefinition mapResultSet(ResultSet resultSet) throws Exception {
        return ContractDefinition.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .accessPolicyId(resultSet.getString(statements.getAccessPolicyIdColumn()))
                .contractPolicyId(resultSet.getString(statements.getContractPolicyIdColumn()))
                .selectorExpression(typeManager.readValue(resultSet.getString(statements.getSelectorExpressionColumn()), AssetSelectorExpression.class))
                .build();
    }

    @Override
    public @NotNull Collection<ContractDefinition> findAll() {
        return findAll(QuerySpec.none()).collect(Collectors.toList());
    }

    @Override
    public @NotNull Stream<ContractDefinition> findAll(QuerySpec spec) {
        return transactionContext.execute(() -> {
            Objects.requireNonNull(spec);

            try (var connection = getConnection()) {
                var queryStmt = statements.createQuery(spec);
                var definitions = executeQuery(connection, this::mapResultSet, queryStmt.getQueryAsString(), queryStmt.getParameters());
                return definitions.stream();
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

    private void insertInternal(Connection connection, ContractDefinition definition) {
        transactionContext.execute(() -> {
            executeQuery(connection, statements.getInsertTemplate(),
                    definition.getId(),
                    definition.getAccessPolicyId(),
                    definition.getContractPolicyId(),
                    toJson(definition.getSelectorExpression()));
        });
    }

    private void updateInternal(Connection connection, ContractDefinition definition) {
        Objects.requireNonNull(definition);
        transactionContext.execute(() -> {
            if (!existsById(connection, definition.getId())) {
                throw new EdcPersistenceException(String.format("Cannot update. Contract Definition with ID '%s' does not exist.", definition.getId()));
            }

            executeQuery(connection, statements.getUpdateTemplate(),
                    definition.getId(),
                    definition.getAccessPolicyId(),
                    definition.getContractPolicyId(),
                    toJson(definition.getSelectorExpression()),
                    definition.getId());

        });
    }

    private String toJson(Object object) {
        return typeManager.writeValueAsString(object);
    }

    private boolean existsById(Connection connection, String definitionId) {
        return transactionContext.execute(() -> executeQuery(connection, (resultSet) -> resultSet.getLong(1), statements.getCountTemplate(), definitionId).iterator().next() > 0);
    }

    private ContractDefinition findById(Connection connection, String id) {
        return transactionContext.execute(() -> single(executeQuery(connection, this::mapResultSet, statements.getFindByTemplate(), id)));
    }

    private <T> T single(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        throw new IllegalStateException("Expected result set size of 1 but got " + list.size());

    }

    private DataSource getDataSource() {
        return Objects.requireNonNull(dataSourceRegistry.resolve(dataSourceName), String.format("DataSource %s could not be resolved", dataSourceName));
    }

    private Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
}
