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

package org.eclipse.edc.connector.dataplane.selector.store.sql;

import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.controlplane.dataplane.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.testfixtures.store.DataPlaneInstanceStoreTestBase;
import org.eclipse.edc.connector.dataplane.selector.store.sql.schema.DataPlaneInstanceStatements;
import org.eclipse.edc.connector.dataplane.selector.store.sql.schema.postgres.PostgresDataPlaneInstanceStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class PostgresDataPlaneInstanceStoreTest extends DataPlaneInstanceStoreTestBase {

    private final DataPlaneInstanceStatements statements = new PostgresDataPlaneInstanceStatements();
    private SqlDataPlaneInstanceStore store;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {
        var typeManager = new JacksonTypeManager();

        store = new SqlDataPlaneInstanceStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), statements, typeManager.getMapper(), queryExecutor);
        var schema = TestUtils.getResourceFileContentAsString("dataplane-instance-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getDataPlaneInstanceTable() + " CASCADE");
    }

    @Override
    protected DataPlaneInstanceStore getStore() {
        return store;
    }

    @Test
    void query_byUrl() {
        var doc1 = DataPlaneInstance.Builder.newInstance().id("test-id").url("http://first.com/api").participantContextId("pc").build();
        var doc2 = DataPlaneInstance.Builder.newInstance().id("test-id-2").url("http://second.com/api").participantContextId("pc").build();

        store.save(doc1);
        store.save(doc2);

        var query = QuerySpec.Builder.newInstance().filter(new Criterion("url", "=", "http://second.com/api")).build();

        assertThat(store.query(query)).hasSize(1).first().extracting(DataPlaneInstance::getId).isEqualTo("test-id-2");
    }

}
