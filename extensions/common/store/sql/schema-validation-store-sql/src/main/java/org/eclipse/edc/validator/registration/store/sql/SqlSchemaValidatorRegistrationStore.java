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

package org.eclipse.edc.validator.registration.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;
import org.eclipse.edc.validator.registration.spi.store.SchemaValidatorRegistrationStore;
import org.eclipse.edc.validator.registration.store.sql.schema.SchemaValidatorRegistrationStoreStatements;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;

public class SqlSchemaValidatorRegistrationStore extends AbstractSqlStore implements SchemaValidatorRegistrationStore {

    private final SchemaValidatorRegistrationStoreStatements statements;

    public SqlSchemaValidatorRegistrationStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                               ObjectMapper objectMapper, SchemaValidatorRegistrationStoreStatements statements, QueryExecutor queryExecutor) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = Objects.requireNonNull(statements);
    }

    @Override
    public SchemaValidatorRegistration findById(String id) {
        return transactionContext.execute(() -> {
            var query = QuerySpec.Builder.newInstance().filter(List.of(new Criterion("id", "=", id))).build();
            try {
                var queryStatement = statements.createQuery(query);
                return queryExecutor.single(getConnection(), true, this::mapResultSet, queryStatement.getQueryAsString(), queryStatement.getParameters());
            } catch (SQLException exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public List<SchemaValidatorRegistration> findByVersionAndValidatedType(String version, String validatedType) {
        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("version", "=", version), new Criterion("validatedType", "=", validatedType)))
                .build();
        try (var stream = findAll(query)) {
            return stream.toList();
        }
    }

    @Override
    public Stream<SchemaValidatorRegistration> findAll(QuerySpec spec) {
        Objects.requireNonNull(spec);
        return transactionContext.execute(() -> {
            try {
                var queryStatement = statements.createQuery(spec);
                return queryExecutor.query(getConnection(), true, this::mapResultSet, queryStatement.getQueryAsString(), queryStatement.getParameters());
            } catch (SQLException exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public StoreResult<SchemaValidatorRegistration> create(SchemaValidatorRegistration registration) {
        Objects.requireNonNull(registration);
        return transactionContext.execute(() -> {
            if (findById(registration.getId()) != null) {
                return StoreResult.alreadyExists(format(ALREADY_EXISTS, registration.getId()));
            }
            insert(registration);
            return StoreResult.success(registration);
        });
    }

    @Override
    public StoreResult<SchemaValidatorRegistration> update(SchemaValidatorRegistration registration) {
        Objects.requireNonNull(registration);
        return transactionContext.execute(() -> {
            if (findById(registration.getId()) == null) {
                return StoreResult.notFound(format(NOT_FOUND, registration.getId()));
            }
            try (var connection = getConnection()) {
                queryExecutor.execute(connection, statements.getUpdateTemplate(),
                        registration.getVersion(),
                        registration.getValidatedType(),
                        registration.getSchema(),
                        toJson(registration.getProfiles()),
                        registration.getUpdatedAt(),
                        registration.getId());
                return StoreResult.success(registration);
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    @Override
    public StoreResult<SchemaValidatorRegistration> delete(String id) {
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var entity = findById(id);
                if (entity == null) {
                    return StoreResult.notFound(format(NOT_FOUND, id));
                }
                queryExecutor.execute(connection, statements.getDeleteTemplate(), id);
                return StoreResult.success(entity);
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    private void insert(SchemaValidatorRegistration registration) {
        try (var connection = getConnection()) {
            queryExecutor.execute(connection, statements.getInsertTemplate(),
                    registration.getId(),
                    registration.getVersion(),
                    registration.getValidatedType(),
                    registration.getSchema(),
                    toJson(registration.getProfiles()),
                    registration.getCreatedAt(),
                    registration.getUpdatedAt());
        } catch (Exception e) {
            throw new EdcPersistenceException(e.getMessage(), e);
        }
    }

    private SchemaValidatorRegistration mapResultSet(ResultSet resultSet) throws SQLException {
        return SchemaValidatorRegistration.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .version(resultSet.getString(statements.getVersionColumn()))
                .validatedType(resultSet.getString(statements.getValidatedTypeColumn()))
                .schema(resultSet.getString(statements.getSchemaColumn()))
                .profiles(fromJson(resultSet.getString(statements.getProfilesColumn()), listOf(String.class)))
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .updatedAt(resultSet.getLong(statements.getUpdatedAtColumn()))
                .build();
    }
}
