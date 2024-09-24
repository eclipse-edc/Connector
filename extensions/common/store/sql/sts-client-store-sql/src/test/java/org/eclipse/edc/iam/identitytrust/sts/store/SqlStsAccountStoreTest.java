/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.sts.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures.StsAccountStoreTestBase;
import org.eclipse.edc.iam.identitytrust.sts.store.schema.BaseSqlDialectStatements;
import org.eclipse.edc.iam.identitytrust.sts.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class SqlStsAccountStoreTest extends StsAccountStoreTestBase {

    private final BaseSqlDialectStatements sqlStatements = new PostgresDialectStatements();

    private SqlStsAccountStore stsClientStore;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension setupExtension, QueryExecutor queryExecutor) {
        var typeManager = new JacksonTypeManager();
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        stsClientStore = new SqlStsAccountStore(setupExtension.getDataSourceRegistry(), setupExtension.getDatasourceName(),
                setupExtension.getTransactionContext(), new ObjectMapper(), sqlStatements, queryExecutor);

        var schema = TestUtils.getResourceFileContentAsString("sts-client-schema.sql");
        setupExtension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension setupExtension) {
        setupExtension.runQuery("DROP TABLE " + sqlStatements.getStsClientTable() + " CASCADE");
    }

    @Override
    protected StsAccountStore getStsClientStore() {
        return stsClientStore;
    }
}
