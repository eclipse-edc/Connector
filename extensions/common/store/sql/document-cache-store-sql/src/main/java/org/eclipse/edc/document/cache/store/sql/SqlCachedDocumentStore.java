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

package org.eclipse.edc.document.cache.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.document.cache.spi.CachedDocument;
import org.eclipse.edc.document.cache.spi.CachedDocumentType;
import org.eclipse.edc.document.cache.spi.PullStrategy;
import org.eclipse.edc.document.cache.spi.store.CachedDocumentStore;
import org.eclipse.edc.document.cache.store.sql.schema.CachedDocumentStoreStatements;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;

public class SqlCachedDocumentStore extends AbstractSqlStore implements CachedDocumentStore {

    private final CachedDocumentStoreStatements statements;

    public SqlCachedDocumentStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                       ObjectMapper objectMapper, CachedDocumentStoreStatements statements, QueryExecutor queryExecutor) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = Objects.requireNonNull(statements);
    }

    @Override
    public CachedDocument findById(String id) {
        return findByField("id", id);
    }

    @Override
    public CachedDocument findByUrl(String url) {
        return findByField("url", url);
    }

    @Override
    public Stream<CachedDocument> findAll(QuerySpec spec) {
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
    public StoreResult<CachedDocument> create(CachedDocument context) {
        Objects.requireNonNull(context);
        return transactionContext.execute(() -> {
            if (findById(context.getId()) != null) {
                return StoreResult.alreadyExists(format(ALREADY_EXISTS, context.getId()));
            }
            if (findByUrl(context.getUrl()) != null) {
                return StoreResult.alreadyExists(format(URL_ALREADY_EXISTS, context.getUrl()));
            }
            insert(context);
            return StoreResult.success(context);
        });
    }

    @Override
    public StoreResult<CachedDocument> update(CachedDocument context) {
        Objects.requireNonNull(context);
        return transactionContext.execute(() -> {
            if (findById(context.getId()) == null) {
                return StoreResult.notFound(format(NOT_FOUND, context.getId()));
            }
            try (var connection = getConnection()) {
                queryExecutor.execute(connection, statements.getUpdateTemplate(),
                        context.getUrl(),
                        context.getContent(),
                        context.getType().value(),
                        context.getPullStrategy().value(),
                        context.getUpdatedAt(),
                        context.getId());
                return StoreResult.success(context);
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }

    @Override
    public StoreResult<CachedDocument> delete(String id) {
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

    private CachedDocument findByField(String field, String value) {
        return transactionContext.execute(() -> {
            var query = QuerySpec.Builder.newInstance().filter(List.of(new Criterion(field, "=", value))).build();
            try {
                var queryStatement = statements.createQuery(query);
                return queryExecutor.single(getConnection(), true, this::mapResultSet, queryStatement.getQueryAsString(), queryStatement.getParameters());
            } catch (SQLException exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    private void insert(CachedDocument context) {
        try (var connection = getConnection()) {
            queryExecutor.execute(connection, statements.getInsertTemplate(),
                    context.getId(),
                    context.getUrl(),
                    context.getContent(),
                    context.getType().value(),
                    context.getPullStrategy().value(),
                    context.getCreatedAt(),
                    context.getUpdatedAt());
        } catch (Exception e) {
            throw new EdcPersistenceException(e.getMessage(), e);
        }
    }

    private CachedDocument mapResultSet(ResultSet resultSet) throws SQLException {
        return CachedDocument.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .url(resultSet.getString(statements.getUrlColumn()))
                .content(resultSet.getString(statements.getContentColumn()))
                .type(CachedDocumentType.fromValue(resultSet.getString(statements.getDocumentTypeColumn())))
                .pullStrategy(PullStrategy.fromValue(resultSet.getString(statements.getPullStrategyColumn())))
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .updatedAt(resultSet.getLong(statements.getUpdatedAtColumn()))
                .build();
    }
}
