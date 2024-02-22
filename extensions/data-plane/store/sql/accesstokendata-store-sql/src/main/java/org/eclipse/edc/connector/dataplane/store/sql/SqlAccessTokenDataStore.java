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

package org.eclipse.edc.connector.dataplane.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.connector.dataplane.store.sql.schema.AccessTokenDataStatements;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * SQL implementation of {@link DataPlaneStore}
 */
public class SqlAccessTokenDataStore extends AbstractSqlStore implements AccessTokenDataStore {

    private final AccessTokenDataStatements statements;

    public SqlAccessTokenDataStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                   AccessTokenDataStatements statements, ObjectMapper objectMapper, QueryExecutor queryExecutor) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public AccessTokenData getById(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findByIdInternal(connection, id);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> store(AccessTokenData accessTokenData) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, accessTokenData.id()) != null) {
                    return StoreResult.alreadyExists(OBJECT_EXISTS.formatted(accessTokenData.id()));
                }
                insert(connection, accessTokenData);
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> deleteById(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) == null) {
                    return StoreResult.notFound(OBJECT_NOT_FOUND.formatted(id));
                }
                var sql = statements.getDeleteTemplate();
                queryExecutor.execute(connection, sql, id);
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public Collection<AccessTokenData> query(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try (var conn = getConnection()) {
                var sql = statements.createQuery(querySpec);
                return queryExecutor.query(conn, true, this::mapAccessTokenData, sql.getQueryAsString(), sql.getParameters()).toList();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private void insert(Connection connection, AccessTokenData dataFlow) {
        var sql = statements.getInsertTemplate();
        queryExecutor.execute(connection, sql,
                dataFlow.id(),
                toJson(dataFlow.claimToken()),
                toJson(dataFlow.dataAddress())
        );
    }


    private AccessTokenData mapAccessTokenData(ResultSet resultSet) throws SQLException {
        var claimToken = fromJson(resultSet.getString(statements.getClaimTokenColumn()), ClaimToken.class);
        var dataAddress = fromJson(resultSet.getString(statements.getDataAddressColumn()), DataAddress.class);
        var id = resultSet.getString(statements.getIdColumn());

        return new AccessTokenData(id, claimToken, dataAddress);
    }

    private @Nullable AccessTokenData findByIdInternal(Connection conn, String id) {
        return transactionContext.execute(() -> {
            var querySpec = QuerySpec.Builder.newInstance().filter(criterion("id", "=", id)).build();
            var statement = statements.createQuery(querySpec);
            return queryExecutor.query(conn, true, this::mapAccessTokenData, statement.getQueryAsString(), statement.getParameters())
                    .findFirst().orElse(null);
        });
    }

}
