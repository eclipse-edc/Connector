/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector.store.sql;

import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.store.sql.schema.DataPlaneInstanceStatements;
import org.eclipse.edc.connector.dataplane.selector.store.sql.schema.postgres.PostgresDataPlaneInstanceStatements;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper;
import org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilderProvider;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

/**
 * Extensions that expose an implementation of {@link DataPlaneInstanceStore} that uses SQL as backend storage
 */
@Provides(DataPlaneInstanceStore.class)
@Extension(value = SqlDataPlaneInstanceStoreExtension.NAME)
public class SqlDataPlaneInstanceStoreExtension implements ServiceExtension {
    public static final String NAME = "Sql Data Plane Instance Store";


    @Setting(description = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.dataplaneinstance.datasource")
    private String dataSourceName;

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject(required = false)
    private DataPlaneInstanceStatements statements;

    @Inject
    private TypeManager typeManager;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private Clock clock;

    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Inject
    private SqlLeaseContextBuilderProvider leaseContextBuilderProvider;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DataPlaneInstanceStore dataPlaneInstanceStore(ServiceExtensionContext context) {
        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "dataplane-instance-schema.sql");
        var leaseContextBuilder = leaseContextBuilderProvider.createContextBuilder(getStatementImpl().getDataPlaneInstanceTable());
        return new SqlDataPlaneInstanceStore(dataSourceRegistry, dataSourceName, transactionContext,
                getStatementImpl(), leaseContextBuilder, typeManager.getMapper(), queryExecutor);
    }

    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private DataPlaneInstanceStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDataPlaneInstanceStatements(leaseContextBuilderProvider.getStatements(), clock);
    }

}
