/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.store.sql.participantcontext.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;


/**
 * SQL-based {@link Config} store intended for use with PostgreSQL.
 * <p>
 * Note that {@link #merge(ParticipantContextConfiguration)} guards the read-modify-write with a {@code SELECT ... FOR
 * UPDATE} row lock, which is only held for the duration of the enclosing transaction. It therefore requires a real
 * {@link TransactionContext}: under a {@code NoopTransactionContext} with a non-enlisted {@code DataSource} every
 * statement auto-commits, the lock is released immediately, and concurrent merges can lose entries.
 */
public class SqlParticipantContextConfigStore extends AbstractSqlStore implements ParticipantContextConfigStore {

    private static final String EMPTY_JSON_OBJECT = "{}";

    private final ParticipantContextConfigStoreStatements statements;


    public SqlParticipantContextConfigStore(DataSourceRegistry dataSourceRegistry,
                                            String dataSourceName,
                                            TransactionContext transactionContext,
                                            ObjectMapper objectMapper,
                                            QueryExecutor queryExecutor,
                                            ParticipantContextConfigStoreStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public void save(ParticipantContextConfiguration config) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var stmt = statements.getUpsertTemplate();
                queryExecutor.execute(connection, stmt,
                        config.getParticipantContextId(),
                        config.getCreatedAt(),
                        config.getLastModified(),
                        toJson(config.getEntries()),
                        toJson(config.getPrivateEntries())
                );

            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public ParticipantContextConfiguration merge(ParticipantContextConfiguration patch) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                // materialize an empty row if none exists yet, so that the subsequent lock always has a row to take
                queryExecutor.execute(connection, statements.getInsertIfAbsentTemplate(),
                        patch.getParticipantContextId(),
                        patch.getCreatedAt(),
                        patch.getLastModified(),
                        EMPTY_JSON_OBJECT,
                        EMPTY_JSON_OBJECT
                );

                // take an exclusive row lock, held until the enclosing transaction completes. Under READ COMMITTED a
                // blocked reader re-reads the latest committed row once the lock is granted, which is what makes the
                // read-modify-write below safe against concurrent merges.
                var existing = queryExecutor.single(connection, false, this::mapResultSet,
                        statements.getFindByIdForUpdateTemplate(), patch.getParticipantContextId());

                if (existing == null) {
                    throw new EdcPersistenceException("Configuration for participant context '%s' vanished while merging"
                            .formatted(patch.getParticipantContextId()));
                }

                var merged = patch.mergeOnto(existing);

                queryExecutor.execute(connection, statements.getUpdateEntriesTemplate(),
                        merged.getLastModified(),
                        toJson(merged.getEntries()),
                        toJson(merged.getPrivateEntries()),
                        merged.getParticipantContextId()
                );

                return merged;
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public ParticipantContextConfiguration get(String participantContextId) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var query = statements.getFindByIdTemplate();
                return queryExecutor.single(connection, true, this::mapResultSet, query, participantContextId);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private ParticipantContextConfiguration mapResultSet(ResultSet resultSet) throws Exception {
        var participantContextId = resultSet.getString(statements.getIdColumn());
        var created = resultSet.getLong(statements.getCreateTimestampColumn());
        var lastModified = resultSet.getLong(statements.getLastModifiedTimestampColumn());
        Map<String, String> config = fromJson(resultSet.getString(statements.getEntriesColumn()), getTypeRef());
        Map<String, String> privateConfig = fromJson(resultSet.getString(statements.getPrivateEntriesColumn()), getTypeRef());
        return ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId(participantContextId)
                .createdAt(created)
                .lastModified(lastModified)
                .entries(config)
                .privateEntries(privateConfig)
                .build();
    }
}
