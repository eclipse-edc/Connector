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
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Map;


/**
 * SQL-based {@link Config} store intended for use with PostgreSQL
 */
public class SqlParticipantContextConfigStore extends AbstractSqlStore implements ParticipantContextConfigStore {

    private final ParticipantContextConfigStoreStatements statements;
    private final Clock clock;


    public SqlParticipantContextConfigStore(DataSourceRegistry dataSourceRegistry,
                                            String dataSourceName,
                                            TransactionContext transactionContext,
                                            ObjectMapper objectMapper,
                                            QueryExecutor queryExecutor,
                                            ParticipantContextConfigStoreStatements statements, Clock clock) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
        this.clock = clock;
    }

    @Override
    public void save(String participantContextId, Config config) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var stmt = statements.getUpsertTemplate();
                var timestamp = clock.millis();
                queryExecutor.execute(connection, stmt,
                        participantContextId,
                        timestamp,
                        timestamp,
                        toJson(config.getEntries())
                );

            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public Config get(String participantContextId) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var query = statements.getFindByIdTemplate();
                return queryExecutor.single(connection, true, this::mapResultSet, query, participantContextId);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private Config mapResultSet(ResultSet resultSet) throws Exception {
        Map<String, String> config = fromJson(resultSet.getString(statements.getConfigColumn()), getTypeRef());
        return ConfigFactory.fromMap(config);
    }
}
