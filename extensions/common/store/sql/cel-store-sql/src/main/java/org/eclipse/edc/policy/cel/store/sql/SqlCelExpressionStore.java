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

package org.eclipse.edc.policy.cel.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.cel.store.CelExpressionStore;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.spi.result.StoreResult.alreadyExists;
import static org.eclipse.edc.spi.result.StoreResult.success;


/**
 * SQL-based {@link CelExpression} store intended for use with PostgreSQL
 */
public class SqlCelExpressionStore extends AbstractSqlStore implements CelExpressionStore {

    private final CelExpressionStoreStatements statements;


    public SqlCelExpressionStore(DataSourceRegistry dataSourceRegistry,
                                 String dataSourceName,
                                 TransactionContext transactionContext,
                                 ObjectMapper objectMapper,
                                 QueryExecutor queryExecutor,
                                 CelExpressionStoreStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    private CelExpression mapResultSet(ResultSet resultSet) throws Exception {
        var id = resultSet.getString(statements.getIdColumn());
        List<String> scopes = fromJson(resultSet.getString(statements.getScopesColumn()), getTypeRef());
        var leftOperand = resultSet.getString(statements.getLeftOperandColumn());
        var expression = resultSet.getString(statements.getExpressionColumn());
        var description = resultSet.getString(statements.getDescriptionColumn());
        var created = resultSet.getLong(statements.getCreateTimestampColumn());
        var lastModified = resultSet.getLong(statements.getLastModifiedTimestampColumn());

        return CelExpression.Builder.newInstance()
                .id(id)
                .scopes(new HashSet<>(scopes))
                .leftOperand(leftOperand)
                .expression(expression)
                .description(description)
                .createdAt(created)
                .updatedAt(lastModified)
                .build();
    }

    @Override
    public StoreResult<Void> create(CelExpression expression) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, expression.getId()) != null) {
                    return alreadyExists(alreadyExistsErrorMessage(expression.getId()));
                }

                var stmt = statements.getInsertTemplate();
                queryExecutor.execute(connection, stmt,
                        expression.getId(),
                        expression.getLeftOperand(),
                        expression.getExpression(),
                        expression.getDescription(),
                        toJson(expression.getScopes()),
                        expression.getCreatedAt(),
                        expression.getUpdatedAt()
                );
                return success();

            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> update(CelExpression expression) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, expression.getId()) != null) {
                    queryExecutor.execute(connection,
                            statements.getUpdateTemplate(),
                            expression.getLeftOperand(),
                            expression.getExpression(),
                            expression.getDescription(),
                            toJson(expression.getScopes()),
                            expression.getCreatedAt(),
                            expression.getUpdatedAt(),
                            expression.getId());
                    return StoreResult.success();
                }
                return StoreResult.notFound(notFoundErrorMessage(expression.getId()));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> delete(String id) {
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) != null) {
                    var stmt = statements.getDeleteByIdTemplate();
                    queryExecutor.execute(connection, stmt, id);
                    return success();
                }
                return StoreResult.notFound(notFoundErrorMessage(id));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public List<CelExpression> query(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var query = statements.createQuery(querySpec);
                return queryExecutor.query(connection, true, this::mapResultSet, query.getQueryAsString(), query.getParameters()).toList();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private CelExpression findByIdInternal(Connection connection, String id) {
        return transactionContext.execute(() -> {
            var stmt = statements.getFindByIdTemplate();
            return queryExecutor.single(connection, false, this::mapResultSet, stmt, id);
        });
    }
}
