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

package org.eclipse.edc.connector.store.sql.participantcontext;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
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
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.spi.result.StoreResult.alreadyExists;
import static org.eclipse.edc.spi.result.StoreResult.success;


/**
 * SQL-based {@link ParticipantContext} store intended for use with PostgreSQL
 */
public class SqlParticipantContextStore extends AbstractSqlStore implements ParticipantContextStore {

    private final ParticipantContextStoreStatements statements;

    public SqlParticipantContextStore(DataSourceRegistry dataSourceRegistry,
                                      String dataSourceName,
                                      TransactionContext transactionContext,
                                      ObjectMapper objectMapper,
                                      QueryExecutor queryExecutor,
                                      ParticipantContextStoreStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public StoreResult<Void> create(ParticipantContext participantContext) {
        var id = participantContext.getParticipantContextId();
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) != null) {
                    return alreadyExists(alreadyExistsErrorMessage(id));
                }

                var stmt = statements.getInsertTemplate();
                queryExecutor.execute(connection, stmt,
                        participantContext.getParticipantContextId(),
                        participantContext.getCreatedAt(),
                        participantContext.getLastModified(),
                        participantContext.getState(),
                        toJson(participantContext.getProperties())
                );
                return success();

            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Collection<ParticipantContext>> query(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var query = statements.createQuery(querySpec);
                return success(queryExecutor.query(connection, true, this::mapResultSet, query.getQueryAsString(), query.getParameters()).toList());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> update(ParticipantContext participantContext) {
        var id = participantContext.getParticipantContextId();

        Objects.requireNonNull(participantContext);
        Objects.requireNonNull(id);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, id) != null) {
                    queryExecutor.execute(connection,
                            statements.getUpdateTemplate(),
                            id,
                            participantContext.getCreatedAt(),
                            participantContext.getLastModified(),
                            participantContext.getState(),
                            toJson(participantContext.getProperties()),
                            id);
                    return StoreResult.success();
                }
                return StoreResult.notFound(notFoundErrorMessage(id));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> deleteById(String id) {
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

    private ParticipantContext findByIdInternal(Connection connection, String id) {
        return transactionContext.execute(() -> {
            var stmt = statements.getFindByIdTemplate();
            return queryExecutor.single(connection, false, this::mapResultSet, stmt, id);
        });
    }

    private ParticipantContext mapResultSet(ResultSet resultSet) throws Exception {

        var id = resultSet.getString(statements.getIdColumn());
        var created = resultSet.getLong(statements.getCreateTimestampColumn());
        var lastmodified = resultSet.getLong(statements.getLastModifiedTimestampColumn());
        var state = resultSet.getInt(statements.getStateColumn());
        Map<String, Object> props = fromJson(resultSet.getString(statements.getPropertiesColumn()), getTypeRef());

        return ParticipantContext.Builder.newInstance()
                       .participantContextId(id)
                       .createdAt(created)
                       .lastModified(lastmodified)
                       .state(ParticipantContextState.values()[state])
                       .properties(props)
                       .build();
    }
}
