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

package org.eclipse.edc.jtivalidation.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.jtivalidation.store.sql.schema.JtiValidationStoreStatements;
import org.eclipse.edc.jwt.validation.jti.JtiValidationEntry;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public class SqlJtiValidationStore extends AbstractSqlStore implements JtiValidationStore {

    private final JtiValidationStoreStatements statements;
    private final Monitor monitor;

    public SqlJtiValidationStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                 ObjectMapper objectMapper, JtiValidationStoreStatements statements, QueryExecutor queryExecutor, Monitor monitor) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
        this.monitor = monitor;
    }

    @Override
    public StoreResult<Void> storeEntry(JtiValidationEntry entry) {
        return transactionContext.execute(() -> {
            var stmt = statements.getInsertTemplate();
            try (var connection = getConnection()) {

                if (findByIdInternal(connection, entry.tokenId()) != null) {
                    return StoreResult.alreadyExists("JTI Validation Entry with ID '%s' already exists".formatted(entry.tokenId()));
                }
                queryExecutor.execute(connection, stmt, entry.tokenId(), entry.expirationTimestamp());
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public JtiValidationEntry findById(String id, boolean autoRemove) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {

                var entry = findByIdInternal(connection, id);
                if (entry != null && autoRemove) {
                    // a failing delete should not impact the lookup
                    deleteById(id).onFailure(f -> monitor.warning("Error deleting entry after lookup: '%s'".formatted(id)));
                }
                return entry;
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
                    return StoreResult.notFound("JTI Validation Entry with ID '%s' not found".formatted(id));
                }
                var stmt = statements.getDeleteByIdTemplate();
                queryExecutor.execute(connection, stmt, id);
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Integer> deleteExpired() {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var stmt = statements.deleteWhereExpiredTemplate();
                var rows = queryExecutor.execute(connection, stmt, Instant.now().toEpochMilli());
                return StoreResult.success(rows);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private JtiValidationEntry findByIdInternal(Connection connection, String id) {
        var stmt = statements.getFindByTemplate();
        return queryExecutor.single(connection, false, this::mapResultSet, stmt, id);
    }

    private JtiValidationEntry mapResultSet(ResultSet resultSet) throws Exception {
        var expiresAt = resultSet.getLong(statements.getExpirationTimeColumn());

        return new JtiValidationEntry(resultSet.getString(statements.getTokenIdColumn()), expiresAt);
    }
}
