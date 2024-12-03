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

package org.eclipse.edc.connector.dataplane.store.sql;

import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.connector.dataplane.spi.testfixtures.store.DataPlaneStoreTestBase;
import org.eclipse.edc.connector.dataplane.store.sql.schema.DataPlaneStatements;
import org.eclipse.edc.connector.dataplane.store.sql.schema.postgres.PostgresDataPlaneStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.LeaseUtil;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;


@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class PostgresDataPlaneStoreTest extends DataPlaneStoreTestBase {

    private final DataPlaneStatements statements = new PostgresDataPlaneStatements();
    private LeaseUtil leaseUtil;
    private SqlDataPlaneStore store;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {

        var typeManager = new JacksonTypeManager();

        var clock = Clock.systemUTC();

        leaseUtil = new LeaseUtil(extension.getTransactionContext(), extension::getConnection, statements, clock);
        store = new SqlDataPlaneStore(extension.getDataSourceRegistry(), extension.getDatasourceName(), extension.getTransactionContext(),
                statements, typeManager.getMapper(), clock, queryExecutor, "test-connector");
        var schema = TestUtils.getResourceFileContentAsString("dataplane-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getDataPlaneTable() + " CASCADE");
    }

    @Override
    protected DataPlaneStore getStore() {
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
