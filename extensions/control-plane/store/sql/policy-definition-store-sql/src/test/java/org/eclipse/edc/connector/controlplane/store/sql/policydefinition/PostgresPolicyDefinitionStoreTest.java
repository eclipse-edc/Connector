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

package org.eclipse.edc.connector.controlplane.store.sql.policydefinition;

import org.eclipse.edc.connector.controlplane.policy.spi.testfixtures.store.PolicyDefinitionStoreTestBase;
import org.eclipse.edc.connector.controlplane.store.sql.policydefinition.store.SqlPolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.store.sql.policydefinition.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

/**
 * This test aims to verify those parts of the policy definition store, that are specific to Postgres, e.g. JSON query
 * operators.
 */
@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class PostgresPolicyDefinitionStoreTest extends PolicyDefinitionStoreTestBase {

    private final PostgresDialectStatements statements = new PostgresDialectStatements();
    private SqlPolicyDefinitionStore sqlPolicyStore;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {
        var typeManager = new JacksonTypeManager();
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        sqlPolicyStore = new SqlPolicyDefinitionStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), statements, queryExecutor);

        var schema = TestUtils.getResourceFileContentAsString("policy-definition-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getPolicyTable() + " CASCADE");
    }

    @Override
    protected SqlPolicyDefinitionStore getPolicyDefinitionStore() {
        return sqlPolicyStore;
    }

}
