/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.store.sql;

import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.connector.dataplane.store.sql.schema.DataPlaneStatements;
import org.eclipse.edc.connector.dataplane.store.sql.schema.postgres.PostgresDataPlaneStatements;
import org.eclipse.edc.runtime.metamodel.annotation.*;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

/**
 * Provides Sql Store for Data Plane Flow Requests states
 */
@Extension(value = SqlDataPlaneStoreExtension.NAME)
@Provides(DataPlaneStore.class)
public class SqlDataPlaneStoreExtension implements ServiceExtension {

    public static final String NAME = "Sql Data Plane Store";

    @Setting(value = "Name of the datasource to use for accessing data plane store")
    private static final String DATASOURCE_SETTING_NAME = "edc.datasource.dataplane.name";
    private static final String DEFAULT_DATASOURCE_NAME = "dataplane";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject(required = false)
    private DataPlaneStatements statements;

    @Inject
    private Clock clock;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DataPlaneStore dataPlaneStore(ServiceExtensionContext context) {
        return new SqlDataPlaneStore(dataSourceRegistry, getDataSourceName(context), transactionContext, getStatementImpl(), typeManager.getMapper(), clock);
    }

    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private DataPlaneStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDataPlaneStatements();
    }

    private String getDataSourceName(ServiceExtensionContext context) {
        return context.getConfig().getString(DATASOURCE_SETTING_NAME, DEFAULT_DATASOURCE_NAME);
    }
}
