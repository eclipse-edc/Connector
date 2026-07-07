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

package org.eclipse.edc.jsonld.cache.store.sql;

import org.eclipse.edc.jsonld.cache.spi.store.CachedJsonLdContextStore;
import org.eclipse.edc.jsonld.cache.store.sql.schema.CachedJsonLdContextStoreStatements;
import org.eclipse.edc.jsonld.cache.store.sql.schema.postgres.PostgresDialectStatements;
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

@Provides(CachedJsonLdContextStore.class)
@Extension("SQL cached JSON-LD context store")
public class SqlCachedJsonLdContextStoreExtension implements ServiceExtension {

    @Setting(description = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.jsonldcache.datasource")
    private String dataSourceName;

    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext transactionContext;
    @Inject(required = false)
    private CachedJsonLdContextStoreStatements statements;
    @Inject
    private TypeManager typeManager;
    @Inject
    private QueryExecutor queryExecutor;
    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var store = new SqlCachedJsonLdContextStore(dataSourceRegistry, dataSourceName, transactionContext,
                typeManager.getMapper(), getStatementImpl(), queryExecutor);

        context.registerService(CachedJsonLdContextStore.class, store);

        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "json-ld-cache-schema.sql");
    }

    private CachedJsonLdContextStoreStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements();
    }
}
