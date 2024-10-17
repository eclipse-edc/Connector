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

package org.eclipse.edc.edr.store.index;


import org.eclipse.edc.edr.store.index.sql.schema.JtiValidationStoreStatements;
import org.eclipse.edc.edr.store.index.sql.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Provides({ JtiValidationStore.class })
@Extension(value = "SQL JTI Validation store")
public class SqlJtiValidationStoreExtension implements ServiceExtension {


    @Setting(value = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE)
    public static final String DATASOURCE_NAME = "edc.sql.store.jti.datasource";
    public static final long DEFAULT_CLEANUP_PERIOD_SECONDS = 60;
    @Setting(value = "The period of the JTI entry reaper thread in seconds", defaultValue = DEFAULT_CLEANUP_PERIOD_SECONDS + "")
    public static final String CLEANUP_PERIOD = "edc.sql.store.jti.cleanup.period";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject(required = false)
    private JtiValidationStoreStatements statements;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private TypeManager typeManager;

    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;
    @Inject
    private ExecutorInstrumentation executorInstrumentation;
    private SqlJtiValidationStore sqlStore;
    private long reaperThreadPeriod;
    private ScheduledFuture<?> reaperThread;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var dataSourceName = context.getConfig().getString(DATASOURCE_NAME, DataSourceRegistry.DEFAULT_DATASOURCE);

        reaperThreadPeriod = context.getSetting(CLEANUP_PERIOD, DEFAULT_CLEANUP_PERIOD_SECONDS);

        sqlStore = new SqlJtiValidationStore(dataSourceRegistry, dataSourceName, transactionContext, typeManager.getMapper(),
                getStatementImpl(), queryExecutor, context.getMonitor());

        context.registerService(JtiValidationStore.class, sqlStore);
        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "jti-validation-schema.sql");
    }

    @Override
    public void start() {
        reaperThread = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), "SQL JTI Validation Reaper Thread")
                .scheduleAtFixedRate(sqlStore::deleteExpired, reaperThreadPeriod, reaperThreadPeriod, TimeUnit.SECONDS);
    }

    @Override
    public void shutdown() {
        reaperThread.cancel(true);
    }

    private JtiValidationStoreStatements getStatementImpl() {
        return statements == null ? new PostgresDialectStatements() : statements;
    }

}
