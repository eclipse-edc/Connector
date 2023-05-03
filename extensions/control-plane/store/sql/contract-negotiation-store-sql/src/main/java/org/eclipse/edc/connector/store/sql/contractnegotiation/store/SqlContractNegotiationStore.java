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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.edc.connector.store.sql.contractnegotiation.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.store.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.ResultSetMapper;
import org.eclipse.edc.sql.lease.SqlLeaseContextBuilder;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuerySingle;

/**
 * SQL-based implementation of the {@link ContractNegotiationStore}
 */
public class SqlContractNegotiationStore extends AbstractSqlStore implements ContractNegotiationStore {


    private final ContractNegotiationStatements statements;
    private final SqlLeaseContextBuilder leaseContext;
    private final Clock clock;

    public SqlContractNegotiationStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext, ObjectMapper objectMapper, ContractNegotiationStatements statements, String connectorId, Clock clock) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper);
        this.statements = statements;
        this.clock = clock;
        leaseContext = SqlLeaseContextBuilder.with(transactionContext, connectorId, statements, clock);
    }

    @Override
    public @Nullable ContractNegotiation findById(String negotiationId) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findInternal(connection, negotiationId);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @Nullable ContractNegotiation findForCorrelationId(String correlationId) {
        return transactionContext.execute(() -> {
            // utilize the generic query api
            var query = QuerySpec.Builder.newInstance().filter(List.of(new Criterion("correlationId", "=", correlationId))).build();
            try (var stream = queryNegotiations(query)) {
                return single(stream.collect(Collectors.toList()));
            }
        });
    }

    @Override
    public @Nullable ContractAgreement findContractAgreement(String contractId) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                return findContractAgreementInternal(connection, contractId);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public void save(ContractNegotiation negotiation) {
        var id = negotiation.getId();
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var existing = findInternal(connection, id);
                if (existing == null) {
                    insert(connection, negotiation);
                } else {
                    leaseContext.withConnection(connection).breakLease(id);
                    update(connection, id, negotiation);
                }
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });

    }

    @Override
    public void delete(String negotiationId) {
        transactionContext.execute(() -> {
            var existing = findById(negotiationId);

            //if exists, attempt delete
            if (existing != null) {
                if (existing.getContractAgreement() != null) {
                    throw new IllegalStateException(format("Cannot delete ContractNegotiation [ID=%s] - ContractAgreement already created.", negotiationId));
                }
                try (var connection = getConnection()) {

                    // attempt to acquire lease - should fail if someone else holds the lease
                    leaseContext.withConnection(connection).acquireLease(negotiationId);

                    var stmt = statements.getDeleteTemplate();
                    executeQuery(connection, stmt, negotiationId);

                    //necessary to delete the row in edc_lease
                    leaseContext.withConnection(connection).breakLease(negotiationId);

                    // return existing;
                } catch (SQLException e) {
                    throw new EdcPersistenceException(e);
                }
            }
        });
    }

    @Override
    public @NotNull Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try {
                var statement = statements.createNegotiationsQuery(querySpec);
                return executeQuery(getConnection(), true, contractNegotiationMapper(), statement.getQueryAsString(), statement.getParameters());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @NotNull Stream<ContractAgreement> queryAgreements(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try {
                var statement = statements.createAgreementsQuery(querySpec);
                return executeQuery(getConnection(), true, this::mapContractAgreement, statement.getQueryAsString(), statement.getParameters());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @NotNull List<ContractNegotiation> nextForState(int state, int max) {
        return transactionContext.execute(() -> {
            var stmt = statements.getNextForStateTemplate();
            try (
                    var connection = getConnection();
                    var stream = executeQuery(connection, true, contractNegotiationWithAgreementMapper(connection), stmt, state, clock.millis(), max)
            ) {
                var negotiations = stream.collect(Collectors.toList());
                negotiations.forEach(cn -> leaseContext.withConnection(connection).acquireLease(cn.getId()));
                return negotiations;
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private ContractAgreement findContractAgreementInternal(Connection connection, String contractId) {
        var stmt = statements.getFindContractAgreementTemplate();
        return executeQuerySingle(connection, false, this::mapContractAgreement, stmt, contractId);
    }

    private @Nullable ContractNegotiation findInternal(Connection connection, String id) {
        var sql = statements.getFindTemplate();
        return executeQuerySingle(connection, false, contractNegotiationMapper(), sql, id);
    }

    private void update(Connection connection, String negotiationId, ContractNegotiation updatedValues) {
        var stmt = statements.getUpdateNegotiationTemplate();

        if (updatedValues.getContractAgreement() != null) {
            upsertAgreement(updatedValues.getContractAgreement());
        }

        executeQuery(connection, stmt,
                updatedValues.getState(),
                updatedValues.getStateCount(),
                updatedValues.getStateTimestamp(),
                updatedValues.getErrorDetail(),
                toJson(updatedValues.getContractOffers()),
                toJson(updatedValues.getCallbackAddresses()),
                toJson(updatedValues.getTraceContext()),
                ofNullable(updatedValues.getContractAgreement()).map(ContractAgreement::getId).orElse(null),
                updatedValues.getUpdatedAt(),
                negotiationId);
    }

    private void insert(Connection connection, ContractNegotiation negotiation) {
        // store negotiation
        String agrId = null;
        var agreement = negotiation.getContractAgreement();
        if (agreement != null) {
            agrId = agreement.getId();
            upsertAgreement(agreement);
        }

        var stmt = statements.getInsertNegotiationTemplate();
        executeQuery(connection, stmt, negotiation.getId(),
                negotiation.getCorrelationId(),
                negotiation.getCounterPartyId(),
                negotiation.getCounterPartyAddress(),
                negotiation.getType().ordinal(),
                negotiation.getProtocol(),
                negotiation.getState(),
                negotiation.getStateCount(),
                negotiation.getStateTimestamp(),
                negotiation.getErrorDetail(),
                agrId,
                toJson(negotiation.getContractOffers()),
                toJson(negotiation.getCallbackAddresses()),
                toJson(negotiation.getTraceContext()),
                negotiation.getCreatedAt(),
                negotiation.getUpdatedAt());


    }

    private void upsertAgreement(ContractAgreement contractAgreement) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var agrId = contractAgreement.getId();

                if (findContractAgreement(agrId) == null) {
                    // insert agreement
                    var sql = statements.getInsertAgreementTemplate();
                    executeQuery(connection, sql, contractAgreement.getId(),
                            contractAgreement.getProviderId(),
                            contractAgreement.getConsumerId(),
                            contractAgreement.getContractSigningDate(),
                            contractAgreement.getContractStartDate(),
                            contractAgreement.getContractEndDate(),
                            contractAgreement.getAssetId(),
                            toJson(contractAgreement.getPolicy())
                    );
                } else {
                    // update agreement
                    var query = statements.getUpdateAgreementTemplate();
                    executeQuery(connection, query, contractAgreement.getProviderId(),
                            contractAgreement.getConsumerId(),
                            contractAgreement.getContractSigningDate(),
                            contractAgreement.getContractStartDate(),
                            contractAgreement.getContractEndDate(),
                            contractAgreement.getAssetId(),
                            toJson(contractAgreement.getPolicy()),
                            agrId);
                }

            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });

    }

    @Nullable
    private <T> T single(List<T> list) {
        if (list.size() > 1) {
            throw new IllegalStateException(getMultiplicityError(1, list.size()));
        }

        return list.isEmpty() ? null : list.get(0);
    }

    private ContractAgreement mapContractAgreement(ResultSet resultSet) throws SQLException {
        return ContractAgreement.Builder.newInstance()
                .id(resultSet.getString(statements.getContractAgreementIdColumn()))
                .providerId(resultSet.getString(statements.getProviderAgentColumn()))
                .consumerId(resultSet.getString(statements.getConsumerAgentColumn()))
                .assetId(resultSet.getString(statements.getAssetIdColumn()))
                .policy(fromJson(resultSet.getString(statements.getPolicyColumn()), new TypeReference<>() {
                }))
                .contractStartDate(resultSet.getLong(statements.getStartDateColumn()))
                .contractEndDate(resultSet.getLong(statements.getEndDateColumn()))
                .contractSigningDate(resultSet.getLong(statements.getSigningDateColumn()))
                //todo
                .build();
    }

    private String getMultiplicityError(int expectedSize, int actualSize) {
        return format("Expected to find %d items, but found %d", expectedSize, actualSize);
    }

    private ResultSetMapper<ContractNegotiation> contractNegotiationMapper() {
        return resultSet -> mapContractNegotiation(resultSet, this::extractContractAgreement);
    }

    private ResultSetMapper<ContractNegotiation> contractNegotiationWithAgreementMapper(Connection connection) {
        return (resultSet -> mapContractNegotiation(resultSet, rs -> {
            var agreementId = rs.getString(statements.getContractAgreementIdFkColumn());
            if (agreementId == null) {
                return null;
            } else {
                return findContractAgreementInternal(connection, agreementId);
            }
        }));
    }

    private ContractNegotiation mapContractNegotiation(ResultSet resultSet, ResultSetMapper<ContractAgreement> agreementMapper) throws Exception {
        return ContractNegotiation.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .counterPartyId(resultSet.getString(statements.getCounterPartyIdColumn()))
                .counterPartyAddress(resultSet.getString(statements.getCounterPartyAddressColumn()))
                .protocol(resultSet.getString(statements.getProtocolColumn()))
                .correlationId(resultSet.getString(statements.getCorrelationIdColumn()))
                .contractAgreement(agreementMapper.mapResultSet(resultSet))
                .state(resultSet.getInt(statements.getStateColumn()))
                .stateCount(resultSet.getInt(statements.getStateCountColumn()))
                .stateTimestamp(resultSet.getLong(statements.getStateTimestampColumn()))
                .contractOffers(fromJson(resultSet.getString(statements.getContractOffersColumn()), new TypeReference<>() {
                }))
                .callbackAddresses(fromJson(resultSet.getString(statements.getCallbackAddressesColumn()), new TypeReference<>() {
                }))
                .errorDetail(resultSet.getString(statements.getErrorDetailColumn()))
                .traceContext(fromJson(resultSet.getString(statements.getTraceContextColumn()), new TypeReference<>() {
                }))
                // will throw an exception if the value is outside the Type.values() range
                .type(ContractNegotiation.Type.values()[resultSet.getInt(statements.getTypeColumn())])
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .updatedAt(resultSet.getLong(statements.getUpdatedAtColumn()))
                .build();
    }

    private ContractAgreement extractContractAgreement(ResultSet resultSet) throws SQLException {
        return resultSet.getString(statements.getContractAgreementIdFkColumn()) == null ? null : mapContractAgreement(resultSet);
    }

}
