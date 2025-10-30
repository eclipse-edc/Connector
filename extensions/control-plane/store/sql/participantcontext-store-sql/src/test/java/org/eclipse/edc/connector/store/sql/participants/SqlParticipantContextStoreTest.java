/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.store.sql.participants;

import org.eclipse.edc.connector.store.sql.participantcontext.ParticipantContextStoreStatements;
import org.eclipse.edc.connector.store.sql.participantcontext.SqlParticipantContextStore;
import org.eclipse.edc.connector.store.sql.participantcontext.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStoreTestBase;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ComponentTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlParticipantContextStoreTest extends ParticipantContextStoreTestBase {

    private final ParticipantContextStoreStatements statements = new PostgresDialectStatements();
    private SqlParticipantContextStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        var typeManager = new JacksonTypeManager();
        store = new SqlParticipantContextStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements);

        var schema = TestUtils.getResourceFileContentAsString("participant-context-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getParticipantContextTable() + " CASCADE");
    }

    @Override
    protected ParticipantContextStore getStore() {
        return store;
    }
}