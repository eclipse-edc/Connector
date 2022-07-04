/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.sql.policy.store;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.sql.policy.store.schema.SqlPolicyStoreStatements;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

public class SqlPolicyDefinitionStore implements PolicyDefinitionStore {

    private final DataSourceRegistry dataSourceRegistry;
    private final String dataSourceName;
    private final TransactionContext transactionContext;
    private final TypeManager typeManager;
    private final SqlPolicyStoreStatements statements;

    public SqlPolicyDefinitionStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                    TypeManager typeManager, SqlPolicyStoreStatements sqlPolicyStoreStatements) {
        this.dataSourceRegistry = Objects.requireNonNull(dataSourceRegistry);
        this.dataSourceName = Objects.requireNonNull(dataSourceName);
        this.transactionContext = Objects.requireNonNull(transactionContext);
        this.typeManager = Objects.requireNonNull(typeManager);
        statements = Objects.requireNonNull(sqlPolicyStoreStatements);
    }

    @Override
    public PolicyDefinition findById(String id) {
        var query = QuerySpec.Builder.newInstance().filter("uid=" + id).build();
        return single(findAll(query).collect(Collectors.toList()));
    }

    @Override
    public Stream<PolicyDefinition> findAll(QuerySpec querySpec) {
        Objects.requireNonNull(querySpec);
        var queryStatement = statements.createQuery(querySpec);

        try (var connection = getConnection()) {
            return executeQuery(connection, this::mapResultSet, queryStatement.getQueryAsString(), queryStatement.getParameters()).stream();
        } catch (Exception exception) {
            throw new EdcPersistenceException(exception);
        }
    }

    @Override
    public void save(PolicyDefinition policy) {
        Objects.requireNonNull(policy);
        transactionContext.execute(() -> {
            if (findById(policy.getUid()) != null) {
                update(policy);
            } else {
                insert(policy);
            }
        });
    }

    @Override
    public @Nullable PolicyDefinition deleteById(String policyId) {
        Objects.requireNonNull(policyId);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var entity = findById(policyId);
                if (entity != null) {
                    executeQuery(connection, statements.getDeleteTemplate(), policyId);
                }
                return entity;
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    private void insert(PolicyDefinition def) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var policy = def.getPolicy();
                var id = def.getUid();
                executeQuery(connection, statements.getInsertTemplate(),
                        id,
                        toJson(policy.getPermissions(), new TypeReference<List<Permission>>() {
                        }),
                        toJson(policy.getProhibitions(), new TypeReference<List<Prohibition>>() {
                        }),
                        toJson(policy.getObligations(), new TypeReference<List<Duty>>() {
                        }),
                        toJson(policy.getExtensibleProperties()),
                        policy.getInheritsFrom(),
                        policy.getAssigner(),
                        policy.getAssignee(),
                        policy.getTarget(),
                        toJson(policy.getType(), new TypeReference<PolicyType>() {
                        }));
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    private void update(PolicyDefinition def) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var policy = def.getPolicy();
                var id = def.getUid();
                executeQuery(connection, statements.getUpdateTemplate(),
                        toJson(policy.getPermissions(), new TypeReference<List<Permission>>() {
                        }),
                        toJson(policy.getProhibitions(), new TypeReference<List<Prohibition>>() {
                        }),
                        toJson(policy.getObligations(), new TypeReference<List<Duty>>() {
                        }),
                        toJson(policy.getExtensibleProperties()),
                        policy.getInheritsFrom(),
                        policy.getAssigner(),
                        policy.getAssignee(),
                        policy.getTarget(),
                        toJson(policy.getType(), new TypeReference<PolicyType>() {
                        }),
                        id);
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    private PolicyDefinition mapResultSet(ResultSet resultSet) throws SQLException {
        var policy = Policy.Builder.newInstance()
                .permissions(fromJson(resultSet.getString(statements.getPermissionsColumn()), new TypeReference<>() {
                }))
                .prohibitions(fromJson(resultSet.getString(statements.getProhibitionsColumn()), new TypeReference<>() {
                }))
                .duties(fromJson(resultSet.getString(statements.getDutiesColumn()), new TypeReference<>() {
                }))
                .extensibleProperties(fromJson(resultSet.getString(statements.getExtensiblePropertiesColumn()), new TypeReference<>() {
                }))
                .inheritsFrom(resultSet.getString(statements.getInheritsFromColumn()))
                .assigner(resultSet.getString(statements.getAssignerColumn()))
                .assignee(resultSet.getString(statements.getAssigneeColumn()))
                .target(resultSet.getString(statements.getTargetColumn()))
                .type(fromJson(resultSet.getString(statements.getTypeColumn()), new TypeReference<>() {
                }))
                .build();
        return PolicyDefinition.Builder.newInstance().uid(resultSet.getString(statements.getPolicyIdColumn()))
                .policy(policy)
                .build();
    }

    /**
     * Returns either a single element from the list, or null if empty. Throws an IllegalStateException if the list has
     * more than 1 element
     */
    @Nullable
    private <T> T single(List<T> list) {
        if (list.size() > 1) {
            throw new IllegalArgumentException("Expected result set size of 1 but got " + list.size());
        }
        return list.isEmpty() ? null : list.get(0);
    }

    private <T> String toJson(Object object, TypeReference<T> typeReference) {
        return typeManager.writeValueAsString(object, typeReference);
    }

    private String toJson(Object object) {
        return typeManager.writeValueAsString(object);
    }

    private <T> T fromJson(String json, TypeReference<T> typeReference) {
        return typeManager.readValue(json, typeReference);
    }

    private DataSource getDataSource() {
        return Objects.requireNonNull(dataSourceRegistry.resolve(dataSourceName), String.format("DataSource %s could not be resolved", dataSourceName));
    }

    private Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
}

