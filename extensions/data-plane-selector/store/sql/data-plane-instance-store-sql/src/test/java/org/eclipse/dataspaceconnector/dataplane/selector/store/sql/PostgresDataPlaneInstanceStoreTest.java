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

package org.eclipse.dataspaceconnector.dataplane.selector.store.sql;

import org.eclipse.dataspaceconnector.common.util.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.dataspaceconnector.dataplane.selector.TestDataPlaneInstance;
import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstanceImpl;
import org.eclipse.dataspaceconnector.dataplane.selector.store.DataPlaneInstanceStore;
import org.eclipse.dataspaceconnector.dataplane.selector.store.DataPlaneInstanceStoreTestBase;
import org.eclipse.dataspaceconnector.dataplane.selector.store.sql.schema.DataPlaneInstanceStatements;
import org.eclipse.dataspaceconnector.dataplane.selector.store.sql.schema.postgres.PostgresDataPlaneInstanceStatements;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.sql.PostgresqlLocalInstance;
import org.eclipse.dataspaceconnector.sql.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;


@PostgresqlDbIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class PostgresDataPlaneInstanceStoreTest extends DataPlaneInstanceStoreTestBase {


    private final DataPlaneInstanceStatements statements = new PostgresDataPlaneInstanceStatements();

    SqlDataPlaneInstanceStore store;

    @BeforeAll
    static void prepare() {
        PostgresqlLocalInstance.createTestDatabase();
    }

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension) throws IOException, SQLException {

        var typeManager = new TypeManager();
        typeManager.registerTypes(DataPlaneInstanceImpl.class);
        typeManager.registerTypes(TestDataPlaneInstance.class);
        

        store = new SqlDataPlaneInstanceStore(extension.getDataSourceRegistry(), extension.getDatasourceName(), extension.getTransactionContext(), statements, typeManager.getMapper());
        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) throws SQLException {
        extension.runQuery("DROP TABLE " + statements.getDataPlaneInstanceTable() + " CASCADE");
    }

    @Override
    protected DataPlaneInstanceStore getStore() {
        return store;
    }
}
