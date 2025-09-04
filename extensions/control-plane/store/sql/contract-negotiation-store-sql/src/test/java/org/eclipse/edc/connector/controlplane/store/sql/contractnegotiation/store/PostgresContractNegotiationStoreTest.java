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

import org.eclipse.edc.connector.controlplane.contract.spi.testfixtures.negotiation.store.ContractNegotiationStoreTestBase;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.BaseSqlLeaseStatements;
import org.eclipse.edc.sql.lease.SqlLeaseContextBuilderImpl;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
import org.eclipse.edc.sql.testfixtures.LeaseUtil;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

/**
 * This test aims to verify those parts of the contract negotiation store, that are specific to Postgres, e.g. JSON
 * query operators.
 */
@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class PostgresContractNegotiationStoreTest extends ContractNegotiationStoreTestBase {

    private final LeaseStatements leaseStatements = new BaseSqlLeaseStatements();
    private final PostgresDialectStatements statements = new PostgresDialectStatements(leaseStatements, Clock.systemUTC());
    private SqlContractNegotiationStore store;
    private LeaseUtil leaseUtil;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {


        var manager = new JacksonTypeManager();

        manager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        var leaseContextBuilder = SqlLeaseContextBuilderImpl.with(extension.getTransactionContext(), CONNECTOR_NAME, statements.getContractNegotiationTable(), leaseStatements, clock, queryExecutor);

        store = new SqlContractNegotiationStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), manager.getMapper(), statements, leaseContextBuilder, queryExecutor);

        var schema = TestUtils.getResourceFileContentAsString("contract-negotiation-schema.sql");
        extension.runQuery(schema);
        leaseUtil = new LeaseUtil(extension.getTransactionContext(), extension::getConnection, statements.getContractNegotiationTable(), leaseStatements, clock);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getContractNegotiationTable() + " CASCADE");
        extension.runQuery("DROP TABLE " + statements.getContractAgreementTable() + " CASCADE");
        extension.runQuery("DROP TABLE " + leaseStatements.getLeaseTableName() + " CASCADE");
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

}
