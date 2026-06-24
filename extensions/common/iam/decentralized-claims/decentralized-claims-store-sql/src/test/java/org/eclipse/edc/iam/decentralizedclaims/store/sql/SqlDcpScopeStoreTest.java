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

package org.eclipse.edc.iam.decentralizedclaims.store.sql;

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.store.DcpScopeStore;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.store.DcpScopeStoreTestBase;
import org.eclipse.edc.iam.decentralizedclaims.store.sql.schema.BaseSqlDcpScopeStatements;
import org.eclipse.edc.iam.decentralizedclaims.store.sql.schema.postgres.PostgresDcpScopeStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlDcpScopeStoreTest extends DcpScopeStoreTestBase {

    private final BaseSqlDcpScopeStatements sqlStatements = new PostgresDcpScopeStatements();
    private SqlDcpScopeStore sqlStore;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension setupExtension, QueryExecutor queryExecutor) {
        sqlStore = new SqlDcpScopeStore(setupExtension.getDataSourceRegistry(), setupExtension.getDatasourceName(),
                setupExtension.getTransactionContext(), sqlStatements, new JacksonTypeManager().getMapper(), queryExecutor);

        var schema = TestUtils.getResourceFileContentAsString("dcp-scope-schema.sql");
        setupExtension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension setupExtension) {
        setupExtension.runQuery("DROP TABLE " + sqlStatements.getTableName() + " CASCADE");
    }

    @Override
    protected DcpScopeStore getStore() {
        return sqlStore;
    }
}
