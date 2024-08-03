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

package org.eclipse.edc.edr.store.index.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndexTestBase;
import org.eclipse.edc.edr.store.index.SqlEndpointDataReferenceEntryIndex;
import org.eclipse.edc.edr.store.index.sql.schema.BaseSqlDialectStatements;
import org.eclipse.edc.edr.store.index.sql.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class SqlEndpointDataReferenceEntryIndexTest extends EndpointDataReferenceEntryIndexTestBase {

    private final BaseSqlDialectStatements statements = new PostgresDialectStatements();

    private SqlEndpointDataReferenceEntryIndex entryIndex;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {

        entryIndex = new SqlEndpointDataReferenceEntryIndex(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), new ObjectMapper(), statements, queryExecutor);
        var schema = TestUtils.getResourceFileContentAsString("edr-index-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getEdrEntryTable() + " CASCADE");
    }

    @Override
    protected EndpointDataReferenceEntryIndex getStore() {
        return entryIndex;
    }
}
