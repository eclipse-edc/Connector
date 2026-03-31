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

package org.eclipse.edc.controlplane.tasks.sql;

import org.eclipse.edc.controlplane.tasks.sql.schema.postgres.PostgresDialectStatementsTask;
import org.eclipse.edc.controlplane.tasks.store.TaskStore;
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

import static org.eclipse.edc.controlplane.tasks.sql.SqlTaskStoreExtension.NAME;

@Extension(NAME)
public class SqlTaskStoreExtension implements ServiceExtension {

    public static final String NAME = "Tasks SQL Store Extension";


    @Setting(description = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.tasks.datasource")
    private String dataSourceName;
    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private TypeManager typeManager;
    @Inject
    private QueryExecutor queryExecutor;
    @Inject(required = false)
    private TaskStatements statements;
    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Override
    public void initialize(ServiceExtensionContext context) {
        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "task-schema.sql");
    }

    @Provider
    public TaskStore createSqlStore() {
        return new SqlTaskStore(dataSourceRegistry, dataSourceName, transactionContext, typeManager.getMapper(), queryExecutor, getStatementImpl());
    }

    private TaskStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatementsTask();
    }
}
