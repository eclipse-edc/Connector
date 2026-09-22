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

import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerGroupStore;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerStore;
import org.eclipse.edc.connector.controlplane.store.sql.partner.schema.postgres.PartnerGroupPostgresDialectStatements;
import org.eclipse.edc.connector.controlplane.store.sql.partner.schema.postgres.PartnerPostgresDialectStatements;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.connector.controlplane.store.sql.partner.SqlPartnerStoreExtension.NAME;

@Extension(value = NAME)
public class SqlPartnerStoreExtension implements ServiceExtension {

    public static final String NAME = "Partner and Partner Group SQL Store Extension";

    @Setting(description = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.partner.datasource")
    private String dataSourceName;

    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private TypeManager typeManager;
    @Inject
    private QueryExecutor queryExecutor;
    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;
    @Inject(required = false)
    private PartnerStoreStatements partnerStatements;
    @Inject(required = false)
    private PartnerGroupStoreStatements partnerGroupStatements;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "partner-schema.sql");
    }

    @Provider
    public PartnerStore partnerStore() {
        var statements = partnerStatements != null ? partnerStatements : new PartnerPostgresDialectStatements();
        return new SqlPartnerStore(dataSourceRegistry, dataSourceName, transactionContext, typeManager.getMapper(), queryExecutor, statements);
    }

    @Provider
    public PartnerGroupStore partnerGroupStore() {
        var statements = partnerGroupStatements != null ? partnerGroupStatements : new PartnerGroupPostgresDialectStatements();
        return new SqlPartnerGroupStore(dataSourceRegistry, dataSourceName, transactionContext, typeManager.getMapper(), queryExecutor, statements);
    }
}
