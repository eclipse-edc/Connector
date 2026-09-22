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
import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerStore;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public class SqlPartnerStore extends AbstractSqlStore implements PartnerStore {

    private final PartnerStoreStatements statements;

    public SqlPartnerStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                           ObjectMapper objectMapper, QueryExecutor queryExecutor, PartnerStoreStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public Partner findById(String participantContextId, String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findByIdInternal(connection, participantContextId, id);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public Partner findByIdentity(String participantContextId, String identity) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findByIdentityInternal(connection, participantContextId, identity);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public List<Partner> query(QuerySpec querySpec) {
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
    public StoreResult<Void> create(Partner partner) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var participantContextId = partner.getParticipantContextId();
                if (findByIdInternal(connection, participantContextId, partner.getId()) != null) {
                    return StoreResult.alreadyExists(format(PARTNER_ALREADY_EXISTS, partner.getId(), participantContextId));
                }
                if (findByIdentityInternal(connection, participantContextId, partner.getIdentity()) != null) {
                    return StoreResult.alreadyExists(format(PARTNER_IDENTITY_ALREADY_EXISTS, partner.getIdentity(), participantContextId));
                }

                queryExecutor.execute(connection, statements.getInsertTemplate(),
                        participantContextId,
                        partner.getId(),
                        partner.getIdentity(),
                        partner.getName(),
                        toJson(partner.getProperties()),
                        toJson(partner.getGroupIds()),
                        partner.getCreatedAt());
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> update(Partner partner) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var participantContextId = partner.getParticipantContextId();
                if (findByIdInternal(connection, participantContextId, partner.getId()) == null) {
                    return StoreResult.notFound(format(PARTNER_NOT_FOUND, partner.getId(), participantContextId));
                }
                var sameIdentity = findByIdentityInternal(connection, participantContextId, partner.getIdentity());
                if (sameIdentity != null && !sameIdentity.getId().equals(partner.getId())) {
                    return StoreResult.alreadyExists(format(PARTNER_IDENTITY_ALREADY_EXISTS, partner.getIdentity(), participantContextId));
                }

                queryExecutor.execute(connection, statements.getUpdateTemplate(),
                        partner.getIdentity(),
                        partner.getName(),
                        toJson(partner.getProperties()),
                        toJson(partner.getGroupIds()),
                        participantContextId,
                        partner.getId());
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
                    return StoreResult.notFound(format(PARTNER_NOT_FOUND, id, participantContextId));
                }
                queryExecutor.execute(connection, statements.getDeleteByIdTemplate(), participantContextId, id);
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private Partner findByIdInternal(Connection connection, String participantContextId, String id) {
        return queryExecutor.single(connection, false, this::mapResultSet, statements.getFindByIdTemplate(), participantContextId, id);
    }

    private Partner findByIdentityInternal(Connection connection, String participantContextId, String identity) {
        return queryExecutor.single(connection, false, this::mapResultSet, statements.getFindByIdentityTemplate(), participantContextId, identity);
    }

    private Partner mapResultSet(ResultSet resultSet) throws Exception {
        Map<String, Object> properties = fromJson(resultSet.getString(statements.getPropertiesColumn()), getTypeRef());
        List<String> groupIds = fromJson(resultSet.getString(statements.getGroupIdsColumn()), getTypeRef());
        return Partner.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .participantContextId(resultSet.getString(statements.getParticipantContextIdColumn()))
                .identity(resultSet.getString(statements.getIdentityColumn()))
                .name(resultSet.getString(statements.getNameColumn()))
                .properties(properties)
                .groupIds(groupIds == null ? new HashSet<>() : new HashSet<>(groupIds))
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .build();
    }
}
