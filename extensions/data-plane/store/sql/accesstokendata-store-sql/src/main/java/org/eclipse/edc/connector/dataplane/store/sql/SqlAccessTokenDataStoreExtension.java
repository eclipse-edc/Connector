/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.store.sql;

import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.connector.dataplane.store.sql.schema.AccessTokenDataStatements;
import org.eclipse.edc.connector.dataplane.store.sql.schema.postgres.PostgresAccessTokenDataStatements;
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
 * Provides Sql Store for {@link AccessTokenData} objects
 */
@Extension(value = SqlAccessTokenDataStoreExtension.NAME)
public class SqlAccessTokenDataStoreExtension implements ServiceExtension {

    public static final String NAME = "Sql AccessTokenData Store";

    @Setting(value = "Name of the datasource to use for accessing data plane store")
    private static final String DATASOURCE_SETTING_NAME = "edc.datasource.accesstokendata.name";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject(required = false)
    private AccessTokenDataStatements statements;

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
    public AccessTokenDataStore dataPlaneStore(ServiceExtensionContext context) {
        var dataSourceName = getDataSourceName(context);
        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "accesstoken-data-schema.sql");
        return new SqlAccessTokenDataStore(dataSourceRegistry, dataSourceName, transactionContext,
                getStatementImpl(), typeManager.getMapper(), queryExecutor);
    }

    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private AccessTokenDataStatements getStatementImpl() {
        return statements != null ? statements : new PostgresAccessTokenDataStatements();
    }

    private String getDataSourceName(ServiceExtensionContext context) {
        return context.getConfig().getString(DATASOURCE_SETTING_NAME, DataSourceRegistry.DEFAULT_DATASOURCE);
    }
}
