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

package org.eclipse.edc.controlplane.tasks.sql;

import org.eclipse.edc.controlplane.tasks.sql.schema.postgres.PostgresDialectStatementsTask;
import org.eclipse.edc.controlplane.tasks.store.TaskStore;
import org.eclipse.edc.controlplane.tasks.store.TaskStoreTestBase;
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
public class SqlTaskStoreTest extends TaskStoreTestBase {
    private final TaskStatements statements = new PostgresDialectStatementsTask();
    private SqlTaskStore store;

    @Override
    protected TaskStore getStore() {
        return store;
    }

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        var typeManager = new JacksonTypeManager();

        typeManager.registerTypes(TestPayload.class);
        store = new SqlTaskStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements);

        var schema = TestUtils.getResourceFileContentAsString("task-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getTaskTable() + " CASCADE");
    }
}
