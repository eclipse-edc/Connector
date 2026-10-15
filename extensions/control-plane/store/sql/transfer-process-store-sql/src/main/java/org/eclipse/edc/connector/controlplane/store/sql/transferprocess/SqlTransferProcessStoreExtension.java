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

package org.eclipse.edc.connector.controlplane.store.sql.transferprocess;

import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.SqlTransferProcessStore;
import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.TransferProcessStoreStatements;
import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
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

import java.time.Clock;
import java.time.Duration;

@Provides(TransferProcessStore.class)
@Extension(value = "SQL transfer process store")
public class SqlTransferProcessStoreExtension implements ServiceExtension {

    @Setting(description = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.transferprocess.datasource")
    private String dataSourceName;

    @Setting(description = "How long a runtime keeps the lease on a transfer process while processing it, in ISO-8601 duration format. " +
            "It must be longer than the slowest processing step, e.g. a protocol message dispatch including HTTP timeouts and retries, " +
            "otherwise the transfer process can be processed again, by another runtime or by the same one",
            key = TransferProcessStore.LEASE_DURATION_SETTING, defaultValue = TransferProcessStore.LEASE_DURATION_DEFAULT_VALUE)
    private Duration leaseDuration;

    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext trxContext;
    @Inject
    private Clock clock;

    @Inject(required = false)
    private TransferProcessStoreStatements statements;

    @Inject
    private TypeManager typeManager;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var store = new SqlTransferProcessStore(dataSourceRegistry, dataSourceName, trxContext,
                typeManager::getMapper, getStatementImpl(), queryExecutor, context.getRuntimeId(), clock, leaseDuration);
        context.registerService(TransferProcessStore.class, store);

        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "transfer-process-schema.sql");
    }

    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private TransferProcessStoreStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements(clock);
    }

}
