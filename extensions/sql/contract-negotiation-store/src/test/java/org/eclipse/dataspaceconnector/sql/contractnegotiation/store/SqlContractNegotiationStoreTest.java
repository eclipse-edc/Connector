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

import org.eclipse.dataspaceconnector.contract.common.ContractId;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.sql.datasource.ConnectionFactoryDataSource;
import org.eclipse.dataspaceconnector.sql.datasource.ConnectionPoolDataSource;
import org.eclipse.dataspaceconnector.sql.lease.LeaseUtil;
import org.eclipse.dataspaceconnector.sql.pool.ConnectionPool;
import org.eclipse.dataspaceconnector.sql.pool.commons.CommonsConnectionPool;
import org.eclipse.dataspaceconnector.sql.pool.commons.CommonsConnectionPoolConfig;
import org.eclipse.dataspaceconnector.transaction.local.DataSourceResource;
import org.eclipse.dataspaceconnector.transaction.local.LocalDataSourceRegistry;
import org.eclipse.dataspaceconnector.transaction.local.LocalTransactionContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.dataspaceconnector.sql.contractnegotiation.TestFunctions.createContract;
import static org.eclipse.dataspaceconnector.sql.contractnegotiation.TestFunctions.createContractBuilder;
import static org.eclipse.dataspaceconnector.sql.contractnegotiation.TestFunctions.createNegotiation;
import static org.eclipse.dataspaceconnector.sql.contractnegotiation.TestFunctions.createPolicy;

class SqlContractNegotiationStoreTest {

    private static final String DATASOURCE_NAME = "contractnegotiation";
    private static final String CONNECTOR_NAME = "test-connector";
    private ConnectionPool connectionPool;
    private SqlContractNegotiationStore store;
    private LocalDataSourceRegistry dataSourceRegistry;
    private LeaseUtil leaseUtil;

    @BeforeEach
    void setUp() throws SQLException {
        var monitor = new Monitor() {
        };
        var txManager = new LocalTransactionContext(monitor);
        dataSourceRegistry = new LocalDataSourceRegistry(txManager);
        var transactionContext = (TransactionContext) txManager;
        var jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setURL("jdbc:h2:mem:");

        var connection = jdbcDataSource.getConnection();
        var dataSource = new ConnectionFactoryDataSource(() -> connection);
        connectionPool = new CommonsConnectionPool(dataSource, CommonsConnectionPoolConfig.Builder.newInstance().build());
        var poolDataSource = new ConnectionPoolDataSource(connectionPool);
        dataSourceRegistry.register(DATASOURCE_NAME, poolDataSource);
        txManager.registerResource(new DataSourceResource(poolDataSource));
        var statements = new PostgresStatements();
        TypeManager manager = new TypeManager();

        manager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));
        store = new SqlContractNegotiationStore(dataSourceRegistry, DATASOURCE_NAME, transactionContext, manager, statements, CONNECTOR_NAME);

        try (var inputStream = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            var schema = new String(Objects.requireNonNull(inputStream).readAllBytes(), StandardCharsets.UTF_8);
            transactionContext.execute(() -> executeQuery(connection, schema));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        leaseUtil = new LeaseUtil(transactionContext, this::getConnection, statements);
    }

    @AfterEach
    void tearDown() throws Exception {
        connectionPool.close();
    }

    @Test
    @DisplayName("Verify that an entity is found by ID")
    void find() {
        var id = "test-cn1";
        var negotiation = createNegotiation(id);
        store.save(negotiation);

        assertThat(store.find(id))
                .usingRecursiveComparison()
                .isEqualTo(negotiation);

    }

    @Test
    @DisplayName("Verify that an entity is found by ID even when leased")
    void find_whenLeased_shouldReturnEntity() {
        var id = "test-cn1";
        var negotiation = createNegotiation(id);
        store.save(negotiation);

        leaseUtil.leaseEntity(id, CONNECTOR_NAME);
        assertThat(store.find(id))
                .usingRecursiveComparison()
                .isEqualTo(negotiation);


        var id2 = "test-cn2";
        var negotiation2 = createNegotiation(id2);
        store.save(negotiation2);

        leaseUtil.leaseEntity(id2, "someone-else");
        assertThat(store.find(id2))
                .usingRecursiveComparison()
                .isEqualTo(negotiation2);

    }

    @Test
    @DisplayName("Verify that null is returned when entity not found")
    void find_notExist() {
        assertThat(store.find("not-exist")).isNull();
    }

    @Test
    @DisplayName("Find entity by its correlation ID")
    void findForCorrelationId() {
        var negotiation = createNegotiation("test-cn1");
        store.save(negotiation);

        assertThat(store.findForCorrelationId(negotiation.getCorrelationId()))
                .usingRecursiveComparison()
                .isEqualTo(negotiation);
    }

    @Test
    @DisplayName("Find ContractAgreement by contract ID")
    void findContractAgreement() {
        var agreement = createContract("test-ca1");
        var negotiation = createNegotiation("test-cn1", agreement);
        store.save(negotiation);

        assertThat(store.findContractAgreement(agreement.getId()))
                .usingRecursiveComparison()
                .isEqualTo(agreement);
    }

    @Test
    @DisplayName("Verify that null is returned if ContractAgreement not found")
    void findContractAgreement_notExist() {
        assertThat(store.findContractAgreement("not-exist")).isNull();
    }

    @Test
    @DisplayName("Verify that entity is stored")
    void save() {
        var negotiation = createNegotiation("test-id1");
        store.save(negotiation);

        assertThat(store.find(negotiation.getId()))
                .usingRecursiveComparison()
                .isEqualTo(negotiation);
    }

    @Test
    @DisplayName("Verify that entity and related entities are stored")
    void save_withContract() {
        var agreement = createContract("test-agreement");
        var negotiation = createNegotiation("test-negotiation", agreement);
        store.save(negotiation);

        var actual = store.find(negotiation.getId());
        assertThat(actual)
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(negotiation);
        assertThat(actual.getContractAgreement()).usingRecursiveComparison().isEqualTo(agreement);
    }

    @Test
    @DisplayName("Verify that an existing entity is updated instead")
    void save_exists_shouldUpdate() {
        var id = "test-id1";
        var negotiation = createNegotiation(id);
        store.save(negotiation);

        var newNegotiation = ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.CONSUMER)
                .id(id)
                .stateCount(420) //modified
                .state(800) //modified
                .correlationId("corr-test-id1")
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("ids-multipart")
                .build();

        store.save(newNegotiation);

        var actual = store.find(negotiation.getId());
        assertThat(actual).isNotNull();
        assertThat(actual.getStateCount()).isEqualTo(420);
        assertThat(actual.getState()).isEqualTo(800);
    }

    @Test
    @DisplayName("Verify that updating an entity breaks the lease")
    void update_leasedBySelf_shouldBreakLease() {
        var id = "test-id1";
        var negotiation = createNegotiation(id);
        store.save(negotiation);

        leaseUtil.leaseEntity(id, CONNECTOR_NAME);

        var newNegotiation = ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.CONSUMER)
                .id(id)
                .stateCount(420) //modified
                .state(800) //modified
                .correlationId("corr-test-id1")
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("ids-multipart")
                .build();

        // update should break lease
        store.save(newNegotiation);

        assertThat(leaseUtil.isLeased(id, CONNECTOR_NAME)).isFalse();


        var next = store.nextForState(800, 10);
        assertThat(next).usingRecursiveFieldByFieldElementComparator().containsOnly(newNegotiation);

    }

    @Test
    @DisplayName("Verify that updating an entity breaks the lease")
    void update_leasedByOther_shouldThrowException() {
        var id = "test-id1";
        var negotiation = createNegotiation(id);
        store.save(negotiation);

        leaseUtil.leaseEntity(id, "someone-else");

        var newNegotiation = ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.CONSUMER)
                .id(id)
                .stateCount(420) //modified
                .state(800) //modified
                .correlationId("corr-test-id1")
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("ids-multipart")
                .build();

        // update should break lease
        assertThat(leaseUtil.isLeased(id, "someone-else")).isTrue();
        assertThatThrownBy(() -> store.save(newNegotiation)).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Should persist the agreement when a negotiation is updated")
    void update_addsAgreement_shouldPersist() {
        var negotiationId = "test-cn1";
        var negotiation = createNegotiation(negotiationId);
        store.save(negotiation);

        // now add the agreement
        var agreement = createContract("test-ca1");
        var updatedNegotiation = createNegotiation(negotiationId, agreement);

        store.save(updatedNegotiation); //should perform an update + insert

        assertThat(store.queryAgreements(QuerySpec.none()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(agreement);

        assertThat(Objects.requireNonNull(store.find(negotiationId)).getContractAgreement()).isEqualTo(agreement);
    }

    @Test
    @DisplayName("Should update the agreement when a negotiation is updated")
    void update_whenAgreementExists_shouldUpdate() {
        var negotiationId = "test-cn1";
        var agreement = createContract("test-ca1");
        var negotiation = createNegotiation(negotiationId, agreement);
        store.save(negotiation);
        var dbNegotiation = store.find(negotiationId);
        assertThat(dbNegotiation.getContractAgreement().getPolicy().getExtensibleProperties()).isEmpty();

        // now add the agreement
        agreement.getPolicy().getExtensibleProperties().put("somekey", "someval");
        store.save(negotiation); //should perform an update + insert


        var updatedNegotiation = store.find(negotiationId);
        assertThat(updatedNegotiation).isNotNull();
        assertThat(updatedNegotiation.getContractAgreement()).isNotNull();
        assertThat(updatedNegotiation.getContractAgreement().getPolicy().getExtensibleProperties()).containsEntry("somekey", "someval");

    }

    @Test
    @DisplayName("Verify that an entity can be deleted")
    void delete() {
        var id = UUID.randomUUID().toString();
        var n = createNegotiation(id);
        store.save(n);

        assertThat(store.find(id)).isNotNull().usingRecursiveComparison().isEqualTo(n);

        //todo: verify returned object
        store.delete(id);

        assertThat(store.find(id)).isNull();
    }

    @Test
    @DisplayName("Verify that an entity cannot be deleted when leased by self")
    void delete_whenLeasedBySelf_shouldThrowException() {
        var id = UUID.randomUUID().toString();
        var n = createNegotiation(id);
        store.save(n);

        leaseUtil.leaseEntity(id, CONNECTOR_NAME);

        //todo: verify returned object
        assertThatThrownBy(() -> store.delete(id)).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Verify that an entity cannot be deleted when leased by other")
    void delete_whenLeasedByOther_shouldThrowException() {
        var id = UUID.randomUUID().toString();
        var n = createNegotiation(id);
        store.save(n);

        leaseUtil.leaseEntity(id, "someone-else");

        //todo: verify returned object
        assertThatThrownBy(() -> store.delete(id)).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Verify that deleting a non-existing entity returns null")
    void delete_notExist() {
        // todo: verify returned object is null
        store.delete("not-exist");
    }

    @Test
    @DisplayName("Verify that attempting to delete a negotiation with a contract raises an exception")
    void delete_contractExists() {
        var id = UUID.randomUUID().toString();
        var contract = createContract("test-agreement");
        var n = createNegotiation(id, contract);
        store.save(n);

        assertThat(store.find(id)).isNotNull().usingRecursiveComparison().isEqualTo(n);

        assertThatThrownBy(() -> store.delete(id)).isInstanceOf(EdcException.class)
                .hasMessageContaining("Cannot delete ContractNegotiation")
                .hasMessageContaining("ContractAgreement already created.");

    }

    @Test
    @DisplayName("Verify that paging is used")
    void queryNegotiations() {
        var querySpec = QuerySpec.Builder.newInstance().limit(10).offset(5).build();

        IntStream.range(0, 100)
                .mapToObj(i -> createNegotiation("" + i))
                .forEach(cn -> store.save(cn));

        var result = store.queryNegotiations(querySpec);

        assertThat(result).hasSize(10)
                .extracting(ContractNegotiation::getId)
                .map(Integer::parseInt)
                .allMatch(i -> i > 4 && i < 15);
    }

    @Test
    @DisplayName("Verify that paging is used")
    void queryNegotiations_withAgreement() {
        var querySpec = QuerySpec.Builder.newInstance().limit(10).offset(5).build();

        IntStream.range(0, 100)
                .mapToObj(i -> {
                    var agreement = createContract("contract" + i);
                    return createNegotiation("" + i, agreement);
                })
                .forEach(cn -> store.save(cn));

        var result = store.queryNegotiations(querySpec);

        assertThat(result).hasSize(10)
                .allMatch(c -> c.getContractAgreement() != null)
                .extracting(ContractNegotiation::getId)
                .map(Integer::parseInt)
                .allMatch(i -> i > 4 && i < 15);
    }

    @Test
    @DisplayName("Verify that out-of-bounds paging parameters produce empty result")
    void queryNegotiations_offsetTooLarge() {
        var querySpec = QuerySpec.Builder.newInstance().limit(10).offset(50).build();

        IntStream.range(0, 10)
                .mapToObj(i -> createNegotiation("" + i))
                .forEach(cn -> store.save(cn));

        var result = store.queryNegotiations(querySpec);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Verify that nextForState returns the correct amount of items")
    void nextForState() {
        var negotiations = IntStream
                .range(0, 10)
                .mapToObj(i -> createNegotiation("id" + i))
                .collect(Collectors.toList());
        negotiations.forEach(store::save);

        var batch = store.nextForState(ContractNegotiationStates.REQUESTED.code(), 5);

        assertThat(batch).hasSize(5).isSubsetOf(negotiations);

    }

    @Test
    @DisplayName("nextForState: verify that only non-leased entities are returned")
    void nextForState_withLeasedEntity() {
        var negotiations = IntStream
                .range(0, 10)
                .mapToObj(i -> createNegotiation(String.valueOf(i)))
                .collect(Collectors.toList());
        negotiations.forEach(store::save);

        // mark a few as "leased"
        store.nextForState(ContractNegotiationStates.REQUESTED.code(), 5);

        var batch2 = store.nextForState(ContractNegotiationStates.REQUESTED.code(), 10);
        assertThat(batch2)
                .hasSize(5)
                .isSubsetOf(negotiations)
                .extracting(ContractNegotiation::getId)
                .map(Integer::parseInt)
                .allMatch(i -> i >= 5);
    }

    @Test
    @DisplayName("nextForState: verify that an expired lease is re-acquired")
    void nextForState_withLeasedEntity_expiredLease() throws InterruptedException {
        var negotiations = IntStream
                .range(0, 5)
                .mapToObj(i -> createNegotiation(String.valueOf(i)))
                .collect(Collectors.toList());
        negotiations.forEach(store::save);

        // mark them as "leased"
        negotiations.forEach(n -> leaseUtil.leaseEntity(n.getId(), CONNECTOR_NAME, Duration.ofMillis(10)));

        // let enough time pass
        Thread.sleep(50);

        var leasedNegotiations = store.nextForState(ContractNegotiationStates.REQUESTED.code(), 5);
        assertThat(leasedNegotiations)
                .hasSize(5)
                .containsAll(negotiations);

        assertThat(leasedNegotiations).allMatch(n -> leaseUtil.isLeased(n.getId(), CONNECTOR_NAME));
    }

    @Test
    void getAgreementsForDefinitionId() {
        var contractAgreement = createContract(ContractId.createContractId("definitionId"));
        var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
        store.save(negotiation);

        var result = store.getAgreementsForDefinitionId("definitionId");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAgreementsForDefinitionId_notFound() {
        var contractAgreement = createContract(ContractId.createContractId("otherDefinitionId"));
        var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
        store.save(negotiation);

        var result = store.getAgreementsForDefinitionId("definitionId");

        assertThat(result).isEmpty();
    }

    @Test
    void queryAgreements_noQuerySpec() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContract(ContractId.createContractId(UUID.randomUUID().toString()));
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            store.save(negotiation);
        });

        var all = store.queryAgreements(QuerySpec.Builder.newInstance().build());

        assertThat(all).hasSize(10);
    }

    @Test
    void queryAgreements_verifyPaging() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContract(ContractId.createContractId(UUID.randomUUID().toString()));
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            store.save(negotiation);
        });

        // page size fits
        assertThat(store.queryAgreements(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4);

        // page size too large
        assertThat(store.queryAgreements(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(5);
    }

    @Test
    void findPolicy_whenNoAgreement() {
        var n = createNegotiation("id1");

        store.save(n);

        var archivedPolicy = store.findPolicyForContract("test-policy");
        assertThat(archivedPolicy).isNull();
    }

    @Test
    void findPolicy_whenAgreement() {
        var policy = createPolicy("test-policy");
        var c = createContractBuilder("test-contract").policy(policy).build();
        var n = createNegotiation("id1", c);

        store.save(n);

        var archivedPolicy = store.findPolicyForContract("test-contract");
        assertThat(archivedPolicy).usingRecursiveComparison().isEqualTo(policy);
    }

    @Test
    void findPolicy_whenMultipleAgreements() {
        var policy = createPolicy("test-policy");
        var c1 = createContractBuilder("test-contract1").policy(policy).build();
        var n1 = createNegotiation("id1", c1);
        var c2 = createContractBuilder("test-contract2").policy(policy).build();
        var n2 = createNegotiation("id2", c2);

        store.save(n1);
        store.save(n2);

        var policies = store.findPolicyForContract("test-contract1");
        assertThat(policies).usingRecursiveComparison().isEqualTo(policy);
    }

    @Test
    void findPolicy_whenAgreement_policyWithRandomId() {
        var expectedPolicy = Policy.Builder.newInstance().build();
        var c = createContractBuilder("test-contract").policy(expectedPolicy).build();
        var n = createNegotiation("id1", c);

        store.save(n);

        var archivedPolicy = store.findPolicyForContract("test-policy");
        assertThat(archivedPolicy).isNull();
        assertThat(store.findContractAgreement("test-contract")).isNotNull().extracting(ContractAgreement::getPolicy).isEqualTo(expectedPolicy);
    }

    private int count(String tableName) {
        return executeQuery(getConnection(), "SELECT COUNT(*) FROM " + tableName);
    }

    private Connection getConnection() {
        try {
            return dataSourceRegistry.resolve(DATASOURCE_NAME).getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}