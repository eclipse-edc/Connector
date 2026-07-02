/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.dataspaceprofile.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.store.sql.dataspaceprofile.store.schema.DataspaceProfileStoreStatements;
import org.eclipse.edc.protocol.spi.DataspaceProfile;
import org.eclipse.edc.protocol.spi.store.DataspaceProfileStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;

public class SqlDataspaceProfileStore extends AbstractSqlStore implements DataspaceProfileStore {

    private final DataspaceProfileStoreStatements statements;
    private final TypeReference<List<String>> stringListType = new TypeReference<>() {
    };

    public SqlDataspaceProfileStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                    ObjectMapper objectMapper, DataspaceProfileStoreStatements statements, QueryExecutor queryExecutor) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = Objects.requireNonNull(statements);
    }

    @Override
    public DataspaceProfile findById(String name) {
        Objects.requireNonNull(name);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findByIdInternal(connection, name);
            } catch (SQLException exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public Stream<DataspaceProfile> findAll(QuerySpec querySpec) {
        Objects.requireNonNull(querySpec);
        return transactionContext.execute(() -> {
            try {
                var queryStatement = statements.createQuery(querySpec);
                return queryExecutor.query(getConnection(), true, this::mapResultSet, queryStatement.getQueryAsString(), queryStatement.getParameters());
            } catch (SQLException exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public StoreResult<DataspaceProfile> create(DataspaceProfile profile) {
        Objects.requireNonNull(profile);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, profile.getName()) != null) {
                    return StoreResult.alreadyExists(format(PROFILE_ALREADY_EXISTS, profile.getName()));
                }
                queryExecutor.execute(connection, statements.getInsertTemplate(),
                        profile.getName(),
                        profile.getProtocolVersion(),
                        profile.getPath(),
                        profile.getBinding(),
                        profile.getNamespace(),
                        toJson(profile.getJsonLdContextsUrl(), stringListType),
                        profile.getCreatedAt());
                return StoreResult.success(profile);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<DataspaceProfile> update(DataspaceProfile profile) {
        Objects.requireNonNull(profile);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, profile.getName()) == null) {
                    return StoreResult.notFound(format(PROFILE_NOT_FOUND, profile.getName()));
                }
                queryExecutor.execute(connection, statements.getUpdateTemplate(),
                        profile.getProtocolVersion(),
                        profile.getPath(),
                        profile.getBinding(),
                        profile.getNamespace(),
                        toJson(profile.getJsonLdContextsUrl(), stringListType),
                        profile.getName());
                return StoreResult.success(profile);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<DataspaceProfile> delete(String name) {
        Objects.requireNonNull(name);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var entity = findByIdInternal(connection, name);
                if (entity != null) {
                    queryExecutor.execute(connection, statements.getDeleteTemplate(), name);
                    return StoreResult.success(entity);
                }
                return StoreResult.notFound(format(PROFILE_NOT_FOUND, name));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private DataspaceProfile findByIdInternal(Connection connection, String name) {
        var query = QuerySpec.Builder.newInstance().filter(new Criterion("name", "=", name)).build();
        var queryStatement = statements.createQuery(query);
        return queryExecutor.single(connection, false, this::mapResultSet, queryStatement.getQueryAsString(), queryStatement.getParameters());
    }

    private DataspaceProfile mapResultSet(ResultSet resultSet) throws SQLException {
        return DataspaceProfile.Builder.newInstance()
                .name(resultSet.getString(statements.getNameColumn()))
                .protocolVersion(resultSet.getString(statements.getProtocolVersionColumn()))
                .path(resultSet.getString(statements.getPathColumn()))
                .binding(resultSet.getString(statements.getBindingColumn()))
                .namespace(resultSet.getString(statements.getNamespaceColumn()))
                .jsonLdContextsUrl(fromJson(resultSet.getString(statements.getJsonLdContextsUrlColumn()), stringListType))
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .build();
    }
}
