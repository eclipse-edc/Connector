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

package org.eclipse.dataspaceconnector.sql.contractnegotiation.store;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.dataspaceconnector.sql.lease.SqlLeaseContextBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

/**
 * SQL-based implementation of the {@link ContractNegotiationStore}
 */
public class SqlContractNegotiationStore implements ContractNegotiationStore {

    private final TypeManager typeManager;
    private final DataSourceRegistry dataSourceRegistry;
    private final String dataSourceName;
    private final TransactionContext transactionContext;
    private final ContractNegotiationStatements statements;
    private final SqlLeaseContextBuilder leaseContext;
    private final Clock clock;

    public SqlContractNegotiationStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext, TypeManager manager, ContractNegotiationStatements statements, String connectorId, Clock clock) {
        typeManager = manager;
        this.dataSourceRegistry = dataSourceRegistry;
        this.dataSourceName = dataSourceName;
        this.transactionContext = transactionContext;
        this.statements = statements;
        this.clock = clock;
        leaseContext = SqlLeaseContextBuilder.with(transactionContext, connectorId, statements, clock);
    }

    @Override
    public @Nullable ContractNegotiation find(String negotiationId) {
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
            return single(queryNegotiations(query).collect(Collectors.toList()));
        });
    }

    @Override
    public @Nullable ContractAgreement findContractAgreement(String contractId) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var stmt = statements.getFindContractAgreementTemplate();

                var contractAgreements = executeQuery(connection, this::mapContractAgreement, stmt, contractId);
                return single(contractAgreements);
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
            var existing = find(negotiationId);

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
            try (var connection = getConnection()) {
                var statement = statements.createNegotiationsQuery(querySpec);
                return executeQuery(connection, this::mapContractNegotiation, statement.getQueryAsString(), statement.getParameters()).stream();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @NotNull Stream<ContractAgreement> getAgreementsForDefinitionId(String definitionId) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var stmt = statements.getFindContractAgreementByDefinitionIdTemplate();

                var contractNegotiation = executeQuery(connection, this::mapContractAgreement, stmt, definitionId + ":%");
                return contractNegotiation.stream();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @NotNull Stream<ContractAgreement> queryAgreements(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var statement = statements.createAgreementsQuery(querySpec);
                return executeQuery(connection, this::mapContractAgreement, statement.getQueryAsString(), statement.getParameters()).stream();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @NotNull List<ContractNegotiation> nextForState(int state, int max) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var stmt = statements.getNextForStateTemplate();
                var negotiations = executeQuery(connection, this::mapContractNegotiation, stmt, state, clock.millis(), max);

                negotiations.forEach(cn -> leaseContext.withConnection(connection).acquireLease(cn.getId()));
                return negotiations;

            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private @Nullable ContractNegotiation findInternal(Connection connection, String id) {
        var stmt = statements.getFindTemplate();

        var contractNegotiation = executeQuery(connection, this::mapContractNegotiation, stmt, id);
        return single(contractNegotiation);
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
                toJson(updatedValues.getTraceContext()),
                ofNullable(updatedValues.getContractAgreement()).map(ContractAgreement::getId).orElse(null),
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
                toJson(negotiation.getTraceContext()));


    }

    private void upsertAgreement(ContractAgreement contractAgreement) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var agrId = contractAgreement.getId();

                if (findContractAgreement(agrId) == null) {
                    // insert agreement
                    var sql = statements.getInsertAgreementTemplate();
                    executeQuery(connection, sql, contractAgreement.getId(),
                            contractAgreement.getProviderAgentId(),
                            contractAgreement.getConsumerAgentId(),
                            contractAgreement.getContractSigningDate(),
                            contractAgreement.getContractStartDate(),
                            contractAgreement.getContractEndDate(),
                            contractAgreement.getAssetId(),
                            toJson(contractAgreement.getPolicy())
                    );
                } else {
                    // update agreement
                    var query = statements.getUpdateAgreementTemplate();
                    executeQuery(connection, query, contractAgreement.getProviderAgentId(),
                            contractAgreement.getConsumerAgentId(),
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
                .providerAgentId(resultSet.getString(statements.getProviderAgentColumn()))
                .consumerAgentId(resultSet.getString(statements.getConsumerAgentColumn()))
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

    private ContractNegotiation mapContractNegotiation(ResultSet resultSet) throws SQLException {
        return ContractNegotiation.Builder.newInstance()
                .id(resultSet.getString(statements.getIdColumn()))
                .counterPartyId(resultSet.getString(statements.getCounterPartyIdColumn()))
                .counterPartyAddress(resultSet.getString(statements.getCounterPartyAddressColumn()))
                .protocol(resultSet.getString(statements.getProtocolColumn()))
                .correlationId(resultSet.getString(statements.getCorrelationIdColumn()))
                .contractAgreement(extractContractAgreement(resultSet))
                .state(resultSet.getInt(statements.getStateColumn()))
                .stateCount(resultSet.getInt(statements.getStateCountColumn()))
                .stateTimestamp(resultSet.getLong(statements.getStateTimestampColumn()))
                .contractOffers(fromJson(resultSet.getString(statements.getContractOffersColumn()), new TypeReference<>() {
                }))
                .errorDetail(resultSet.getString(statements.getErrorDetailColumn()))
                .traceContext(fromJson(resultSet.getString(statements.getTraceContextColumn()), new TypeReference<>() {
                }))
                .build();
    }

    private String toJson(Object object) {
        return typeManager.writeValueAsString(object);
    }

    private <T> T fromJson(String json, TypeReference<T> typeReference) {
        return typeManager.readValue(json, typeReference);
    }

    private ContractAgreement extractContractAgreement(ResultSet resultSet) throws SQLException {
        return resultSet.getString(statements.getContractAgreementIdFkColumn()) == null ? null : mapContractAgreement(resultSet);
    }

    private DataSource getDataSource() {
        return Objects.requireNonNull(dataSourceRegistry.resolve(dataSourceName), format("DataSource %s could not be resolved", dataSourceName));
    }

    private Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
}
