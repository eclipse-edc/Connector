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

package org.eclipse.edc.iam.decentralizedclaims.store.sql;

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.store.DcpScopeStore;
import org.eclipse.edc.iam.decentralizedclaims.store.sql.schema.DcpScopeStatements;
import org.eclipse.edc.iam.decentralizedclaims.store.sql.schema.postgres.PostgresDcpScopeStatements;
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

/**
 * Provides an SQL-backed implementation of {@link DcpScopeStore}.
 */
@Extension(value = SqlDcpScopeStoreExtension.NAME)
public class SqlDcpScopeStoreExtension implements ServiceExtension {

    public static final String NAME = "Sql DCP Scope Store";

    @Setting(description = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.dcpscope.datasource")
    private String dataSourceName;

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject(required = false)
    private DcpScopeStatements statements;

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

    @Override
    public void initialize(ServiceExtensionContext context) {
        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "dcp-scope-schema.sql");
    }

    @Provider
    public DcpScopeStore dcpScopeStore() {
        return new SqlDcpScopeStore(dataSourceRegistry, dataSourceName, transactionContext,
                getStatementImpl(), typeManager.getMapper(), queryExecutor);
    }

    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private DcpScopeStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDcpScopeStatements();
    }
}
