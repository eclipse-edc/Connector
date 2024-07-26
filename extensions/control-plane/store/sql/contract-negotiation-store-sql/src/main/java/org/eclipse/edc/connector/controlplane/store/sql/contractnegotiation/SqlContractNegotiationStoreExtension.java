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

package org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.SqlContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

@Provides({ ContractNegotiationStore.class })
@Extension(value = "SQL contract negotiation store")
public class SqlContractNegotiationStoreExtension implements ServiceExtension {

    public static final String DATASOURCE_NAME_SETTING = "edc.datasource.contractnegotiation.name";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext trxContext;

    @Inject
    private Clock clock;

    @Inject(required = false)
    private ContractNegotiationStatements statements;

    @Inject
    private TypeManager typeManager;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var dataSourceName = getDataSourceName(context);
        var sqlStore = new SqlContractNegotiationStore(dataSourceRegistry, dataSourceName, trxContext,
                typeManager.getMapper(), getStatementImpl(), context.getRuntimeId(), clock, queryExecutor);
        context.registerService(ContractNegotiationStore.class, sqlStore);

        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "contract-negotiation-schema.sql");
    }

    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private ContractNegotiationStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements();
    }

    private String getDataSourceName(ServiceExtensionContext context) {
        return context.getConfig().getString(DATASOURCE_NAME_SETTING, DataSourceRegistry.DEFAULT_DATASOURCE);
    }
}
