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
 * SQL-based {@link Config} store intended for use with PostgreSQL
 */
public class SqlParticipantContextConfigStore extends AbstractSqlStore implements ParticipantContextConfigStore {

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
                        toJson(config.getEntries())
                );

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
        return ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId(participantContextId)
                .createdAt(created)
                .lastModified(lastModified)
                .entries(config)
                .build();
    }
}
