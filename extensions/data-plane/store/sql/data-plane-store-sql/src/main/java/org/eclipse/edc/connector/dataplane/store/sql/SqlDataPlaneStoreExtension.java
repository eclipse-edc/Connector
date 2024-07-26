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

import java.time.Clock;

/**
 * Provides Sql Store for Data Plane Flow Requests states
 */
@Extension(value = SqlDataPlaneStoreExtension.NAME)
public class SqlDataPlaneStoreExtension implements ServiceExtension {

    public static final String NAME = "Sql Data Plane Store";

    @Setting(value = "Name of the datasource to use for accessing data plane store")
    private static final String DATASOURCE_SETTING_NAME = "edc.datasource.dataplane.name";

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

    @Inject
    private QueryExecutor queryExecutor;
    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DataPlaneStore dataPlaneStore(ServiceExtensionContext context) {
        var dataSourceName = getDataSourceName(context);
        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "dataplane-schema.sql");
        return new SqlDataPlaneStore(dataSourceRegistry, dataSourceName, transactionContext,
                getStatementImpl(), typeManager.getMapper(), clock, queryExecutor, context.getRuntimeId());
    }

    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private DataPlaneStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDataPlaneStatements();
    }

    private String getDataSourceName(ServiceExtensionContext context) {
        return context.getConfig().getString(DATASOURCE_SETTING_NAME, DataSourceRegistry.DEFAULT_DATASOURCE);
    }
}
