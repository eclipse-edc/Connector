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

package org.eclipse.edc.catalog.cache.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.catalog.spi.CatalogConstants;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import static java.util.Optional.ofNullable;

public class SqlFederatedCatalogCache extends AbstractSqlStore implements FederatedCatalogCache {

    private final FederatedCatalogCacheStatements statements;

    public SqlFederatedCatalogCache(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                    ObjectMapper objectMapper, QueryExecutor queryExecutor, FederatedCatalogCacheStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public void save(Catalog catalog) {

        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var id = ofNullable(catalog.getProperties().get(CatalogConstants.PROPERTY_ORIGINATOR))
                        .map(Object::toString)
                        .orElse(catalog.getId());

                if (findByIdInternal(connection, id) == null) {
                    insertInternal(connection, id, catalog);
                } else {
                    updateInternal(connection, id, catalog);
                }

            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public Collection<Catalog> query(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var query = statements.createQuery(querySpec);
                return queryExecutor.query(connection, true, this::mapResultSet, query.getQueryAsString(), query.getParameters()).toList();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public void deleteExpired() {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var stmt = statements.getDeleteByMarkedTemplate();
                queryExecutor.execute(connection, stmt, true);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public void expireAll() {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var stmt = statements.getUpdateAsMarkedTemplate();
                queryExecutor.execute(connection, stmt, true);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });

    }

    private Catalog findByIdInternal(Connection connection, String id) {
        var stmt = statements.getFindByIdTemplate();
        return queryExecutor.single(connection, false, this::mapResultSet, stmt, id);
    }

    private void insertInternal(Connection connection, String id, Catalog catalog) {
        var stmt = statements.getInsertTemplate();
        queryExecutor.execute(connection, stmt, id, toJson(catalog), false);
    }

    private void updateInternal(Connection connection, String id, Catalog catalog) {
        var stmt = statements.getUpdateTemplate();
        queryExecutor.execute(connection, stmt, toJson(catalog), false, id);
    }

    private Catalog mapResultSet(ResultSet resultSet) throws Exception {
        var json = resultSet.getString(statements.getCatalogColumn());
        return fromJson(json, Catalog.class);
    }
}
