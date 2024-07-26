/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Microsoft Corporation - Refactoring and improvements
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.assetindex;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.asset.spi.index.DataAddressResolver;
import org.eclipse.edc.connector.controlplane.store.sql.assetindex.schema.AssetStatements;
import org.eclipse.edc.connector.controlplane.store.sql.assetindex.schema.postgres.PostgresDialectStatements;
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

@Provides({ AssetIndex.class, DataAddressResolver.class })
@Extension(value = "SQL asset index")
public class SqlAssetIndexServiceExtension implements ServiceExtension {

    /**
     * Name of the datasource to use for accessing assets.
     */
    @Setting(required = true)
    public static final String DATASOURCE_SETTING_NAME = "edc.datasource.asset.name";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject(required = false)
    private AssetStatements dialect;

    @Inject
    private TypeManager typeManager;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var dataSourceName = context.getConfig().getString(DATASOURCE_SETTING_NAME, DataSourceRegistry.DEFAULT_DATASOURCE);

        var sqlAssetLoader = new SqlAssetIndex(dataSourceRegistry, dataSourceName, transactionContext, typeManager.getMapper(), getDialect(), queryExecutor);

        context.registerService(AssetIndex.class, sqlAssetLoader);
        context.registerService(DataAddressResolver.class, sqlAssetLoader);

        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "asset-index-schema.sql");
    }

    private AssetStatements getDialect() {
        return dialect != null ? dialect : new PostgresDialectStatements();
    }
}
