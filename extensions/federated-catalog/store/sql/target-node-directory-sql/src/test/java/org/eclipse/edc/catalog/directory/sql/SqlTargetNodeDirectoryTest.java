/*
 *  Copyright (c) 2024 Amadeus IT Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.directory.sql;

import org.eclipse.edc.catalog.directory.sql.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.catalog.spi.testfixtures.TargetNodeDirectoryTestBase;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class SqlTargetNodeDirectoryTest extends TargetNodeDirectoryTestBase {

    private final TargetNodeStatements statements = new PostgresDialectStatements();

    private TargetNodeDirectory store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {
        var typeManager = new JacksonTypeManager();
        typeManager.registerTypes(Catalog.class, Dataset.class);
        store = new SqlTargetNodeDirectory(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements);

        var schema = TestUtils.getResourceFileContentAsString("target-node-directory-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getTargetNodeDirectoryTable());
    }

    @Override
    protected TargetNodeDirectory getStore() {
        return store;
    }
}
