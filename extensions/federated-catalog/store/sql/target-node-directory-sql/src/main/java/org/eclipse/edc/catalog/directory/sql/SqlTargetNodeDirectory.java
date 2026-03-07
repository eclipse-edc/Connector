/*
 *  Copyright (c) 2024 Amadeus IT Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.directory.sql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SqlTargetNodeDirectory extends AbstractSqlStore implements TargetNodeDirectory {

    private final TargetNodeStatements statements;

    public SqlTargetNodeDirectory(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                  ObjectMapper objectMapper, QueryExecutor queryExecutor, TargetNodeStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public List<TargetNode> getAll() {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var query = statements.createQuery(QuerySpec.max());
                return queryExecutor.query(connection, true, this::mapResultSet, query.getQueryAsString(), query.getParameters()).toList();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public void insert(TargetNode node) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var id = node.id();

                if (findByIdInternal(connection, id) == null) {
                    insertInternal(connection, id, node);
                } else {
                    updateInternal(connection, id, node);
                }

            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public TargetNode remove(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var existing = findByIdInternal(connection, id);
                if (existing == null) {
                    return null;
                }

                var stmt = statements.getDeleteTemplate();
                queryExecutor.execute(connection, stmt, id);
                return existing;
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private TargetNode findByIdInternal(Connection connection, String id) {
        var stmt = statements.getFindByIdTemplate();
        return queryExecutor.single(connection, false, this::mapResultSet, stmt, id);
    }

    private void insertInternal(Connection connection, String id, TargetNode targetNode) {
        var stmt = statements.getInsertTemplate();
        queryExecutor.execute(connection,
                stmt,
                id,
                targetNode.name(),
                targetNode.targetUrl(),
                toJson(targetNode.supportedProtocols())
        );
    }

    private void updateInternal(Connection connection, String id, TargetNode targetNode) {
        var stmt = statements.getUpdateTemplate();
        queryExecutor.execute(connection,
                stmt,
                targetNode.name(),
                targetNode.targetUrl(),
                toJson(targetNode.supportedProtocols()),
                id
        );
    }

    private TargetNode mapResultSet(ResultSet resultSet) throws Exception {
        return new TargetNode(
                resultSet.getString(statements.getNameColumn()),
                resultSet.getString(statements.getIdColumn()),
                resultSet.getString(statements.getTargetUrlColumn()),
                fromJson(resultSet.getString(statements.getSupportedProtocolsColumn()), new TypeReference<>() {
                })
        );
    }

}
