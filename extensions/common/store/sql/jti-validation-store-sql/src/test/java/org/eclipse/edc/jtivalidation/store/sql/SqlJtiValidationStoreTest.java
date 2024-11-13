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

package org.eclipse.edc.jtivalidation.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.jtivalidation.store.sql.schema.BaseSqlDialectStatements;
import org.eclipse.edc.jtivalidation.store.sql.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStoreTestBase;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.mock;

@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class SqlJtiValidationStoreTest extends JtiValidationStoreTestBase {

    private final BaseSqlDialectStatements statements = new PostgresDialectStatements();

    private SqlJtiValidationStore store;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        store = new SqlJtiValidationStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), new ObjectMapper(), statements, queryExecutor, mock());
        var schema = TestUtils.getResourceFileContentAsString("jti-validation-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getJtiValidationTable() + " CASCADE");
    }


    @Override
    protected JtiValidationStore getStore() {
        return store;
    }
}
