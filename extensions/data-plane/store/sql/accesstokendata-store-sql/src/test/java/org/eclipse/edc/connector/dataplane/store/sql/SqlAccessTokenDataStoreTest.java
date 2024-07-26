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

package org.eclipse.edc.connector.dataplane.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataTestBase;
import org.eclipse.edc.connector.dataplane.store.sql.schema.BaseSqlAccessTokenStatements;
import org.eclipse.edc.connector.dataplane.store.sql.schema.postgres.PostgresAccessTokenDataStatements;
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

@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlAccessTokenDataStoreTest extends AccessTokenDataTestBase {

    private final BaseSqlAccessTokenStatements sqlStatements = new PostgresAccessTokenDataStatements();
    private SqlAccessTokenDataStore sqlStore;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension setupExtension, QueryExecutor queryExecutor) throws IOException {
        var typeManager = new JacksonTypeManager();
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        sqlStore = new SqlAccessTokenDataStore(setupExtension.getDataSourceRegistry(), setupExtension.getDatasourceName(),
                setupExtension.getTransactionContext(), sqlStatements, new ObjectMapper(), queryExecutor);

        var schema = TestUtils.getResourceFileContentAsString("accesstoken-data-schema.sql");
        setupExtension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension setupExtension) {
        setupExtension.runQuery("DROP TABLE " + sqlStatements.getTableName() + " CASCADE");
    }

    @Override
    protected AccessTokenDataStore getStore() {
        return sqlStore;
    }


}
