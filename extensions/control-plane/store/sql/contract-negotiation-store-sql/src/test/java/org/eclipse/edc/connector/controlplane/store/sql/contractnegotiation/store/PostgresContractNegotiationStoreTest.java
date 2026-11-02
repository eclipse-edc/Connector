/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store;

import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.testfixtures.negotiation.store.ContractNegotiationStoreTestBase;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.edc.sql.testfixtures.RowLeaseUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.testfixtures.negotiation.store.TestFunctions.createAgreement;
import static org.eclipse.edc.connector.controlplane.contract.spi.testfixtures.negotiation.store.TestFunctions.createNegotiation;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_LEASED;

/**
 * This test aims to verify those parts of the contract negotiation store, that are specific to Postgres, e.g. JSON
 * query operators.
 */
@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class PostgresContractNegotiationStoreTest extends ContractNegotiationStoreTestBase {

    private final PostgresDialectStatements statements = new PostgresDialectStatements(Clock.systemUTC());
    private SqlContractNegotiationStore store;
    private RowLeaseUtil leaseUtil;
    private PostgresqlStoreSetupExtension extension;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        this.extension = extension;
        var manager = new JacksonTypeManager();

        manager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        store = new SqlContractNegotiationStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), manager::getMapper, statements, queryExecutor, CONNECTOR_NAME, clock);

        var schema = TestUtils.getResourceFileContentAsString("contract-negotiation-schema.sql");
        extension.runQuery(schema);
        leaseUtil = new RowLeaseUtil(extension.getTransactionContext(), extension::getConnection, statements, statements.getContractNegotiationTable(), clock);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getContractNegotiationTable() + " CASCADE");
        extension.runQuery("DROP TABLE " + statements.getContractAgreementTable() + " CASCADE");
    }

    @Override
    protected SqlContractNegotiationStore getContractNegotiationStore() {
        return store;
    }

    @Override
    protected void leaseEntity(String negotiationId, String owner, Duration duration) {
        leaseUtil.leaseEntity(negotiationId, owner, duration);
    }

    @Override
    protected boolean isLeasedBy(String negotiationId, String owner) {
        return leaseUtil.isLeased(negotiationId, owner);
    }

    @Test
    void save_shouldNotWriteAgreement_whenLeasedByOther() {
        var negotiation = createNegotiation("id1");
        store.save(negotiation);
        leaseEntity(negotiation.getId(), "someone-else");
        var agreement = createAgreement(ContractOfferId.create("definition", "asset"));
        var withAgreement = createNegotiation(negotiation.getId(), agreement);

        var result = store.save(withAgreement);

        assertThat(result.failed()).isTrue();
        assertThat(result.reason()).isEqualTo(ALREADY_LEASED);
        assertThat(store.findContractAgreement(agreement.getId())).isNull();
        assertThat(store.findById(negotiation.getId()).getContractAgreement()).isNull();
    }

    @Test
    void nextNotLeased_shouldReturnNegotiationWithAgreement() {
        var agreement = createAgreement(ContractOfferId.create("definition", "asset"));
        var negotiation = createNegotiation("id1", agreement);
        store.save(negotiation);

        var leased = store.nextNotLeased(1, hasState(negotiation.getState()));

        assertThat(leased).hasSize(1).first().satisfies(n ->
                assertThat(n.getContractAgreement()).usingRecursiveComparison().isEqualTo(agreement));
    }

    @Test
    void deleteById_shouldReleaseLease_whenNegotiationHasAgreement() {
        var negotiation = createNegotiation("id1", createAgreement(ContractOfferId.create("definition", "asset")));
        store.save(negotiation);

        var result = store.deleteById(negotiation.getId());

        assertThat(result.succeeded()).isTrue();
        assertThat(store.findById(negotiation.getId())).isNotNull();
        assertThat(leaseUtil.getLeasedBy(negotiation.getId())).isNull();
    }

}
