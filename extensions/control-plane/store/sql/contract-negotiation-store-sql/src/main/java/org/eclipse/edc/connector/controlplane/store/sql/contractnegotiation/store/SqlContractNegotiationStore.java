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

package org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.entity.ProtocolMessages;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.ResultSetMapper;
import org.eclipse.edc.sql.lease.StatefulEntityWriteCheck;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * SQL-based implementation of the {@link ContractNegotiationStore}
 * The lease is stored directly on the contract negotiation row, entities are fetched with {@code FOR UPDATE SKIP LOCKED}
 * so that concurrent runtimes never lease the same contract negotiation.
 */
public class SqlContractNegotiationStore extends AbstractSqlStore implements ContractNegotiationStore {

    public static final Duration DEFAULT_LEASE_DURATION = Duration.ofSeconds(60);

    private final ContractNegotiationStatements statements;
    private final String leaseHolder;
    private final Clock clock;
    private final Duration leaseDuration;
    private final StatefulEntityWriteCheck writeCheck;

    public SqlContractNegotiationStore(DataSourceRegistry dataSourceRegistry, String dataSourceName,
                                       TransactionContext transactionContext, Supplier<ObjectMapper> objectMapperSupplier,
                                       ContractNegotiationStatements statements, QueryExecutor queryExecutor,
                                       String leaseHolder, Clock clock) {
        this(dataSourceRegistry, dataSourceName, transactionContext, objectMapperSupplier, statements, queryExecutor, leaseHolder, clock, DEFAULT_LEASE_DURATION);
    }

    public SqlContractNegotiationStore(DataSourceRegistry dataSourceRegistry, String dataSourceName,
                                       TransactionContext transactionContext, Supplier<ObjectMapper> objectMapperSupplier,
                                       ContractNegotiationStatements statements, QueryExecutor queryExecutor,
                                       String leaseHolder, Clock clock, Duration leaseDuration) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapperSupplier, queryExecutor);
        this.statements = statements;
        this.leaseHolder = leaseHolder;
        this.clock = clock;
        this.leaseDuration = leaseDuration;
        this.writeCheck = new StatefulEntityWriteCheck(queryExecutor, statements, statements.getContractNegotiationTable(), leaseHolder, clock);
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
    public @NotNull List<ContractNegotiation> nextNotLeased(int max, Criterion... criteria) {
        return transactionContext.execute(() -> {
            var filter = Arrays.stream(criteria).toList();
            var querySpec = QuerySpec.Builder.newInstance().filter(filter).sortField("stateTimestamp").limit(max).build();
            var statement = statements.createNegotiationNextNotLeaseQuery(querySpec);

            try (
                    var connection = getConnection();
                    var stream = queryExecutor.query(connection, false, contractNegotiationMapper(), statement.getQueryAsString(), statement.getParameters())
            ) {
                // rows are locked until the end of the transaction, so the lease can be safely acquired on them
                return stream.toList().stream()
                        .filter(n -> acquireLease(connection, n.getId()))
                        .collect(toList());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<ContractNegotiation> findByIdAndLease(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                // the lease is acquired before reading, so that the returned entity cannot be outdated by a concurrent save
                if (!acquireLease(connection, id)) {
                    if (findInternal(connection, id) == null) {
                        return StoreResult.notFound(format("ContractNegotiation %s not found", id));
                    }
                    return StoreResult.alreadyLeased(format("ContractNegotiation %s is currently leased!", id));
                }
                return StoreResult.success(findInternal(connection, id));
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> save(ContractNegotiation negotiation) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {

                var contractAgreement = negotiation.getContractAgreement();
                if (contractAgreement != null) {
                    // the agreement is written before the negotiation (FK), so the lease and the version must be
                    // verified upfront to avoid writing the agreement of a negotiation that cannot be updated. The
                    // negotiation row stays locked until the transaction ends, so that a concurrent writer cannot change
                    // it between the check and the write below
                    var writable = writeCheck.checkAndLock(connection, negotiation.getId(), negotiation.getVersion());
                    if (writable.failed()) {
                        return writable;
                    }

                    var sql = statements.getUpsertAgreementTemplate();

                    queryExecutor.execute(connection, sql,
                            contractAgreement.getId(),
                            contractAgreement.getProviderId(),
                            contractAgreement.getConsumerId(),
                            contractAgreement.getContractSigningDate(),
                            contractAgreement.getAssetId(),
                            toJson(contractAgreement.getPolicy()),
                            contractAgreement.getParticipantContextId(),
                            contractAgreement.getAgreementId(),
                            toJson(contractAgreement.getClaims()),
                            contractAgreement.isRetired(),
                            contractAgreement.getRetirementReason(),
                            contractAgreement.getRetirementDate()
                    );
                }

                var stmt = statements.getUpsertNegotiationTemplate();

                var updated = queryExecutor.execute(connection, stmt,
                        negotiation.getId(),
                        negotiation.getCorrelationId(),
                        negotiation.getCounterPartyId(),
                        negotiation.getCounterPartyAddress(),
                        negotiation.getType().name(),
                        negotiation.getProtocol(),
                        negotiation.getState(),
                        negotiation.getStateCount(),
                        negotiation.getStateTimestamp(),
                        negotiation.getErrorDetail(),
                        contractAgreement == null ? null : contractAgreement.getId(),
                        toJson(negotiation.getContractOffers()),
                        toJson(negotiation.getCallbackAddresses()),
                        toJson(negotiation.getTraceContext()),
                        negotiation.getCreatedAt(),
                        negotiation.getUpdatedAt(),
                        negotiation.isPending(),
                        toJson(negotiation.getProtocolMessages()),
                        negotiation.getParticipantContextId(),
                        toJson(negotiation.getClaims()),
                        // saving the entity breaks the lease, unless another holder owns it
                        null,
                        null,
                        null,
                        negotiation.getVersion() + 1,
                        // an existing entity is updated only if its version is the expected one
                        negotiation.getVersion(),
                        leaseHolder,
                        clock.millis());

                if (updated == 0) {
                    return writeCheck.explainNotWritten(connection, negotiation.getId(), negotiation.getVersion());
                }
                negotiation.setVersion(negotiation.getVersion() + 1);
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });

    }

    @Override
    public StoreResult<Void> breakLease(ContractNegotiation entity) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var updated = queryExecutor.execute(connection, statements.getBreakLeaseTemplate(statements.getContractNegotiationTable()),
                        entity.getId(), leaseHolder, clock.millis());
                if (updated == 0 && findInternal(connection, entity.getId()) != null) {
                    return StoreResult.alreadyLeased(format("ContractNegotiation %s is leased by another holder, cannot break the lease!", entity.getId()));
                }
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
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
    public StoreResult<Void> deleteById(String negotiationId) {

        return transactionContext.execute(() -> {
            var existing = findById(negotiationId);
            if (existing == null) {
                return StoreResult.notFound(format("ContractNegotiation %s not found", negotiationId));
            }
            //if exists, attempt delete
            try (var connection = getConnection()) {

                // attempt to acquire lease - should fail if the contract negotiation is currently leased
                if (!acquireLease(connection, negotiationId)) {
                    return StoreResult.alreadyLeased(format("ContractNegotiation %s is currently leased!", negotiationId));
                }

                var stmt = statements.getDeleteTemplate();
                var deleted = queryExecutor.execute(connection, stmt, negotiationId);
                if (deleted == 0) {
                    // negotiations with an agreement are not deleted, their lease must be released
                    queryExecutor.execute(connection, statements.getBreakLeaseTemplate(statements.getContractNegotiationTable()),
                            negotiationId, leaseHolder, clock.millis());
                }
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public @NotNull Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try {
                return queryNegotiations(querySpec, getConnection());
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
                return queryExecutor.query(getConnection(), true, this::mapContractAgreement, statement.getQueryAsString(), statement.getParameters());
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> retireAgreement(ContractAgreement agreement) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var rowsUpdated = queryExecutor.execute(connection, statements.getRetireAgreementTemplate(),
                        agreement.isRetired(),
                        agreement.getRetirementReason(),
                        agreement.getRetirementDate(),
                        agreement.getId());
                if (rowsUpdated == 0) {
                    return StoreResult.notFound(format("ContractAgreement %s not found", agreement.getId()));
                }
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private boolean acquireLease(Connection connection, String id) {
        var now = clock.millis();
        var sql = statements.getAcquireLeaseTemplate(statements.getContractNegotiationTable());
        return queryExecutor.execute(connection, sql, leaseHolder, now, leaseDuration.toMillis(), id, now) > 0;
    }

    private Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec, Connection connection) {
        var statement = statements.createNegotiationsQuery(querySpec);
        return queryExecutor.query(connection, true, contractNegotiationMapper(), statement.getQueryAsString(), statement.getParameters());
    }

    private ContractAgreement findContractAgreementInternal(Connection connection, String contractId) {
        var stmt = statements.getFindContractAgreementTemplate();
        return queryExecutor.single(connection, false, this::mapContractAgreement, stmt, contractId);
    }

    private @Nullable ContractNegotiation findInternal(Connection connection, String id) {
        var sql = statements.getFindTemplate();
        return queryExecutor.single(connection, false, contractNegotiationMapper(), sql, id);
    }

    private ContractAgreement mapContractAgreement(ResultSet resultSet) throws SQLException {
        return ContractAgreement.Builder.newInstance()
                .id(resultSet.getString(statements.getContractAgreementIdColumn()))
                .providerId(resultSet.getString(statements.getProviderAgentColumn()))
                .consumerId(resultSet.getString(statements.getConsumerAgentColumn()))
                .assetId(resultSet.getString(statements.getAssetIdColumn()))
                .contractSigningDate(resultSet.getLong(statements.getSigningDateColumn()))
                .policy(fromJson(resultSet.getString(statements.getPolicyColumn()), Policy.class))
                .participantContextId(resultSet.getString(statements.getAgreementParticipantContextIdColumn()))
                .agreementId(resultSet.getString(statements.getContractAgreementContractIdColumn()))
                .claims(fromJson(resultSet.getString(statements.getClaimsColumn()), getTypeRef()))
                .retired(resultSet.getBoolean(statements.getRetiredColumn()))
                .retirementReason(resultSet.getString(statements.getRetirementReasonColumn()))
                .retirementDate(resultSet.getLong(statements.getRetirementDateColumn()))
                .build();
    }

    private ResultSetMapper<ContractNegotiation> contractNegotiationMapper() {
        return resultSet -> mapContractNegotiation(resultSet, this::extractContractAgreement);
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
                .type(ContractNegotiation.Type.valueOf(resultSet.getString(statements.getTypeColumn())))
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .updatedAt(resultSet.getLong(statements.getUpdatedAtColumn()))
                .pending(resultSet.getBoolean(statements.getPendingColumn()))
                .protocolMessages(fromJson(resultSet.getString(statements.getProtocolMessagesColumn()), ProtocolMessages.class))
                .participantContextId(resultSet.getString(statements.getParticipantContextIdColumn()))
                .claims(fromJson(resultSet.getString(statements.getNegotiationClaimsColumn()), getTypeRef()))
                .version(resultSet.getLong(statements.getVersionColumn()))
                .build();
    }

    private ContractAgreement extractContractAgreement(ResultSet resultSet) throws SQLException {
        return resultSet.getString(statements.getContractAgreementIdFkColumn()) == null ? null : mapContractAgreement(resultSet);
    }

}
