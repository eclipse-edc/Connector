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

package org.eclipse.edc.connector.controlplane.store.sql.partner;

import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerStore;
import org.eclipse.edc.connector.controlplane.partner.spi.testfixtures.store.PartnerStoreTestBase;
import org.eclipse.edc.connector.controlplane.store.sql.partner.schema.postgres.PartnerPostgresDialectStatements;
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
class PostgresPartnerStoreTest extends PartnerStoreTestBase {

    private final PartnerStoreStatements statements = new PartnerPostgresDialectStatements();
    private SqlPartnerStore store;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        var typeManager = new JacksonTypeManager();
        store = new SqlPartnerStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements);

        extension.runQuery(TestUtils.getResourceFileContentAsString("partner-schema.sql"));
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getPartnerTable() + " CASCADE");
        extension.runQuery("DROP TABLE edc_partner_group CASCADE");
    }

    @Override
    protected PartnerStore getStore() {
        return store;
    }
}
