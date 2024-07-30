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
 *
 */

package org.eclipse.edc.connector.policy.monitor.store.sql;

import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.connector.policy.monitor.spi.testfixtures.store.PolicyMonitorStoreTestBase;
import org.eclipse.edc.connector.policy.monitor.store.sql.schema.PolicyMonitorStatements;
import org.eclipse.edc.connector.policy.monitor.store.sql.schema.PostgresPolicyMonitorStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.testfixtures.LeaseUtil;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;


@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class PostgresPolicyMonitorStoreTest extends PolicyMonitorStoreTestBase {

    private final PolicyMonitorStatements statements = new PostgresPolicyMonitorStatements();
    private LeaseUtil leaseUtil;
    private SqlPolicyMonitorStore store;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {

        var typeManager = new JacksonTypeManager();

        var clock = Clock.systemUTC();

        leaseUtil = new LeaseUtil(extension.getTransactionContext(), extension::getConnection, statements, clock);
        store = new SqlPolicyMonitorStore(extension.getDataSourceRegistry(), extension.getDatasourceName(), extension.getTransactionContext(),
                statements, typeManager.getMapper(), clock, queryExecutor, "test-connector");
        var schema = TestUtils.getResourceFileContentAsString("policy-monitor-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getPolicyMonitorTable() + " CASCADE");
    }

    @Override
    protected PolicyMonitorStore getStore() {
        return store;
    }

    @Override
    protected void leaseEntity(String negotiationId, String owner, Duration duration) {
        getLeaseUtil().leaseEntity(negotiationId, owner, duration);
    }

    @Override
    protected boolean isLeasedBy(String negotiationId, String owner) {
        return getLeaseUtil().isLeased(negotiationId, owner);
    }

    protected LeaseUtil getLeaseUtil() {
        return leaseUtil;
    }
}
