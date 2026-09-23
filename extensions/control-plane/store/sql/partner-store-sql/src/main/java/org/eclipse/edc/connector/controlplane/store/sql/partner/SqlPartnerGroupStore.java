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

package org.eclipse.edc.connector.controlplane.store.sql.partner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerGroupStore;
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
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public class SqlPartnerGroupStore extends AbstractSqlStore implements PartnerGroupStore {

    private final PartnerGroupStoreStatements statements;

    public SqlPartnerGroupStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                ObjectMapper objectMapper, QueryExecutor queryExecutor, PartnerGroupStoreStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public PartnerGroup findById(String participantContextId, String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findByIdInternal(connection, participantContextId, id);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public List<PartnerGroup> query(QuerySpec querySpec) {
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
    public StoreResult<Void> create(PartnerGroup group) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var participantContextId = group.getParticipantContextId();
                if (findByIdInternal(connection, participantContextId, group.getId()) != null) {
                    return StoreResult.alreadyExists(format(PARTNER_GROUP_ALREADY_EXISTS, group.getId(), participantContextId));
                }
                queryExecutor.execute(connection, statements.getInsertTemplate(),
                        participantContextId,
                        group.getId(),
                        group.getName(),
                        group.getDescription(),
                        toJson(group.getProperties()),
                        group.getCreatedAt());
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> update(PartnerGroup group) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var participantContextId = group.getParticipantContextId();
                if (findByIdInternal(connection, participantContextId, group.getId()) == null) {
                    return StoreResult.notFound(format(PARTNER_GROUP_NOT_FOUND, group.getId(), participantContextId));
                }
                queryExecutor.execute(connection, statements.getUpdateTemplate(),
                        group.getName(),
                        group.getDescription(),
                        toJson(group.getProperties()),
                        participantContextId,
                        group.getId());
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> deleteById(String participantContextId, String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (findByIdInternal(connection, participantContextId, id) == null) {
                    return StoreResult.notFound(format(PARTNER_GROUP_NOT_FOUND, id, participantContextId));
                }
                queryExecutor.execute(connection, statements.getDeleteByIdTemplate(), participantContextId, id);
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private PartnerGroup findByIdInternal(Connection connection, String participantContextId, String id) {
        return queryExecutor.single(connection, false, this::mapResultSet, statements.getFindByIdTemplate(), participantContextId, id);
    }

    private PartnerGroup mapResultSet(ResultSet resultSet) throws Exception {
        Map<String, Object> properties = fromJson(resultSet.getString(statements.getPropertiesColumn()), getTypeRef());
        return PartnerGroup.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .participantContextId(resultSet.getString(statements.getParticipantContextIdColumn()))
                .name(resultSet.getString(statements.getNameColumn()))
                .description(resultSet.getString(statements.getDescriptionColumn()))
                .properties(properties)
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .build();
    }
}
