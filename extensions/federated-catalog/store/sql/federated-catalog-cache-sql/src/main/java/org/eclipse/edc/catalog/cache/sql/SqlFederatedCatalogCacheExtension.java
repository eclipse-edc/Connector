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

package org.eclipse.edc.catalog.cache.sql;

import org.eclipse.edc.catalog.cache.sql.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
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

@Provides(FederatedCatalogCache.class)
@Extension(value = "SQL federated catalog cache")
public class SqlFederatedCatalogCacheExtension implements ServiceExtension {

    @Setting(description = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.federatedcatalog.datasource")
    private String dataSourceName;

    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext trxContext;
    @Inject(required = false)
    private FederatedCatalogCacheStatements statements;
    @Inject
    private TypeManager typeManager;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Override
    public void initialize(ServiceExtensionContext context) {
        typeManager.registerTypes(Catalog.class, Dataset.class);
        var store = new SqlFederatedCatalogCache(dataSourceRegistry, dataSourceName, trxContext,
                typeManager.getMapper(), queryExecutor, getStatementImpl());
        context.registerService(FederatedCatalogCache.class, store);
        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "cache-schema.sql");
    }

    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private FederatedCatalogCacheStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements();
    }

}
