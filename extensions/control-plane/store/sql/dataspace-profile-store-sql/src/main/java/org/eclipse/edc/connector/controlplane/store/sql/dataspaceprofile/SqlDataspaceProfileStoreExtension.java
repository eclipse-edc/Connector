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

package org.eclipse.edc.connector.controlplane.store.sql.dataspaceprofile;

import org.eclipse.edc.connector.controlplane.store.sql.dataspaceprofile.store.SqlDataspaceProfileStore;
import org.eclipse.edc.connector.controlplane.store.sql.dataspaceprofile.store.schema.DataspaceProfileStoreStatements;
import org.eclipse.edc.connector.controlplane.store.sql.dataspaceprofile.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.protocol.spi.store.DataspaceProfileStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

@Provides(DataspaceProfileStore.class)
@Extension("SQL dataspace profile store")
public class SqlDataspaceProfileStoreExtension implements ServiceExtension {

    @Setting(description = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.dataspaceprofile.datasource")
    private String dataSourceName;

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject(required = false)
    private DataspaceProfileStoreStatements statements;

    @Inject
    private TypeManager typeManager;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var store = new SqlDataspaceProfileStore(dataSourceRegistry, dataSourceName, transactionContext,
                typeManager.getMapper(), getStatementImpl(), queryExecutor);

        context.registerService(DataspaceProfileStore.class, store);

        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "dataspace-profile-schema.sql");
    }

    private DataspaceProfileStoreStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements();
    }
}
