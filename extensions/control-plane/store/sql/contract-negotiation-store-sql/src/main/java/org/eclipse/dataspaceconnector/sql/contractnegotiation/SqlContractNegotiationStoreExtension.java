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

package org.eclipse.dataspaceconnector.sql.contractnegotiation;

import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.sql.contractnegotiation.store.SqlContractNegotiationStore;
import org.eclipse.dataspaceconnector.sql.contractnegotiation.store.schema.ContractNegotiationStatements;
import org.eclipse.dataspaceconnector.sql.contractnegotiation.store.schema.postgres.PostgresDialectStatements;

import java.time.Clock;

@Provides({ ContractNegotiationStore.class })
public class SqlContractNegotiationStoreExtension implements ServiceExtension {

    private static final String DATASOURCE_NAME_SETTING = "edc.datasource.contractnegotiation.name";
    private static final String DEFAULT_DATASOURCE_NAME = "contractnegotiation";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext trxContext;

    @Inject
    private Clock clock;

    @Inject(required = false)
    private ContractNegotiationStatements statements;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var sqlStore = new SqlContractNegotiationStore(dataSourceRegistry, getDataSourceName(context), trxContext, context.getTypeManager(), getStatementImpl(), context.getConnectorId(), clock);
        context.registerService(ContractNegotiationStore.class, sqlStore);
    }

    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private ContractNegotiationStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements();
    }

    private String getDataSourceName(ServiceExtensionContext context) {
        return context.getConfig().getString(DATASOURCE_NAME_SETTING, DEFAULT_DATASOURCE_NAME);
    }
}
