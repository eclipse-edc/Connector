/*
 *  Copyright (c) 2024 Amadeus IT Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.directory.sql;

import org.eclipse.edc.catalog.directory.sql.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
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

@Provides(TargetNodeDirectory.class)
@Extension(value = "SQL target node directory")
public class SqlTargetNodeDirectoryExtension implements ServiceExtension {

    @Setting(description = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.targetnodedirectory.datasource")
    private String dataSourceName;

    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext trxContext;
    @Inject(required = false)
    private TargetNodeStatements statements;
    @Inject
    private TypeManager typeManager;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Override
    public void initialize(ServiceExtensionContext context) {
        typeManager.registerTypes(TargetNode.class);
        var targetNodeDirectory = new SqlTargetNodeDirectory(dataSourceRegistry, dataSourceName, trxContext,
                typeManager.getMapper(), queryExecutor, getStatementImpl());
        context.registerService(TargetNodeDirectory.class, targetNodeDirectory);
        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "target-node-directory-schema.sql");
    }

    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private TargetNodeStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements();
    }

}
