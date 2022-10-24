/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataplane.selector.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;
import org.eclipse.dataspaceconnector.dataplane.selector.store.DataPlaneInstanceStore;
import org.eclipse.dataspaceconnector.dataplane.selector.store.sql.schema.DataPlaneInstanceStatements;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.sql.store.AbstractSqlStore;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuerySingle;

/**
 * SQL store implementation of {@link DataPlaneInstanceStore}
 */
public class SqlDataPlaneInstanceStore extends AbstractSqlStore implements DataPlaneInstanceStore {

    private final DataPlaneInstanceStatements statements;

    public SqlDataPlaneInstanceStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext, DataPlaneInstanceStatements statements, ObjectMapper objectMapper) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper);
        this.statements = Objects.requireNonNull(statements);
    }

    @Override
    public void save(DataPlaneInstance instance) {

        transactionContext.execute(() -> {
            try (var connection = getConnection()) {

                if (findByIdInternal(connection, instance.getId()) == null) {
                    insert(connection, instance);
                } else {
                    update(connection, instance);
                }

            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public void saveAll(Collection<DataPlaneInstance> instances) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                for (var instance : instances) {
                    if (findByIdInternal(connection, instance.getId()) == null) {
                        insert(connection, instance);
                    } else {
                        update(connection, instance);
                    }
                }
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public DataPlaneInstance findById(String id) {
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findByIdInternal(connection, id);

            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public Stream<DataPlaneInstance> getAll() {
        try {
            var sql = statements.getAllTemplate();
            return executeQuery(getConnection(), true, this::mapResultSet, sql);
        } catch (SQLException exception) {
            throw new EdcPersistenceException(exception);
        }
    }


    private DataPlaneInstance findByIdInternal(Connection connection, String id) {
        var sql = statements.getFindByIdTemplate();
        return executeQuerySingle(connection, false, this::mapResultSet, sql, id);
    }

    private void insert(Connection connection, DataPlaneInstance instance) {
        var sql = statements.getInsertTemplate();
        executeQuery(connection, sql, instance.getId(), toJson(instance));
    }

    private void update(Connection connection, DataPlaneInstance instance) {
        var sql = statements.getUpdateTemplate();
        executeQuery(connection, sql, toJson(instance), instance.getId());
    }


    private DataPlaneInstance mapResultSet(ResultSet resultSet) throws Exception {
        var json = resultSet.getString(statements.getDataColumn());
        return fromJson(json, DataPlaneInstance.class);
    }


}
