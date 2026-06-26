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

package org.eclipse.edc.iam.decentralizedclaims.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.store.DcpScopeStore;
import org.eclipse.edc.iam.decentralizedclaims.store.sql.schema.DcpScopeStatements;
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

import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * SQL implementation of {@link DcpScopeStore}.
 */
public class SqlDcpScopeStore extends AbstractSqlStore implements DcpScopeStore {

    private final DcpScopeStatements statements;

    public SqlDcpScopeStore(DataSourceRegistry dataSourceRegistry,
                            String dataSourceName,
                            TransactionContext transactionContext,
                            DcpScopeStatements statements,
                            ObjectMapper objectMapper,
                            QueryExecutor queryExecutor) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public StoreResult<Void> save(DcpScope scope) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, scope.getId()) != null) {
                    update(connection, scope);
                } else {
                    insert(connection, scope);
                }
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> delete(String scopeId) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var deleted = queryExecutor.execute(connection, statements.getDeleteTemplate(), scopeId);
                if (deleted == 0) {
                    return StoreResult.notFound("DcpScope with id %s not found".formatted(scopeId));
                }
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<List<DcpScope>> query(QuerySpec spec) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var query = statements.createQuery(spec);
                var scopes = queryExecutor.query(connection, true, this::mapDcpScope, query.getQueryAsString(), query.getParameters()).toList();
                return StoreResult.success(scopes);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private void insert(Connection connection, DcpScope scope) {
        queryExecutor.execute(connection, statements.getInsertTemplate(),
                scope.getId(),
                scope.getType().name(),
                scope.getValue(),
                scope.getProfile(),
                scope.getPrefixMapping());
    }

    private void update(Connection connection, DcpScope scope) {
        queryExecutor.execute(connection, statements.getUpdateTemplate(),
                scope.getType().name(),
                scope.getValue(),
                scope.getProfile(),
                scope.getPrefixMapping(),
                scope.getId());
    }

    private @Nullable DcpScope findByIdInternal(Connection connection, String id) {
        var query = statements.createQuery(QuerySpec.Builder.newInstance().filter(criterion("id", "=", id)).build());
        return queryExecutor.query(connection, true, this::mapDcpScope, query.getQueryAsString(), query.getParameters())
                .findFirst().orElse(null);
    }

    private DcpScope mapDcpScope(ResultSet resultSet) throws SQLException {
        var type = DcpScope.Type.valueOf(resultSet.getString(statements.getTypeColumn()));
        var builder = DcpScope.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .type(type)
                .value(resultSet.getString(statements.getValueColumn()))
                .profile(resultSet.getString(statements.getProfileColumn()));

        var prefixMapping = resultSet.getString(statements.getPrefixMappingColumn());
        if (prefixMapping != null) {
            builder.prefixMapping(prefixMapping);
        }

        return builder.build();
    }
}
