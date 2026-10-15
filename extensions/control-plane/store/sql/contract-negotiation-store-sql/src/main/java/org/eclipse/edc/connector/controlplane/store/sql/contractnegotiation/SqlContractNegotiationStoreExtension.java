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

@Provides({ContractNegotiationStore.class})
@Extension(value = "SQL contract negotiation store")
public class SqlContractNegotiationStoreExtension implements ServiceExtension {

    @Setting(description = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.contractnegotiation.datasource")
    private String dataSourceName;

    @Setting(description = "How long a runtime keeps the lease on a contract negotiation while processing it, in ISO-8601 duration format. " +
            "It must be longer than the slowest processing step, e.g. a protocol message dispatch including HTTP timeouts and retries, " +
            "otherwise the contract negotiation can be processed again, by another runtime or by the same one",
            key = ContractNegotiationStore.LEASE_DURATION_SETTING, defaultValue = ContractNegotiationStore.LEASE_DURATION_DEFAULT_VALUE)
    private Duration leaseDuration;

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
        var sqlStore = new SqlContractNegotiationStore(dataSourceRegistry, dataSourceName, trxContext,
                typeManager::getMapper, getStatementImpl(), queryExecutor, context.getRuntimeId(), clock, leaseDuration);
        context.registerService(ContractNegotiationStore.class, sqlStore);

        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "contract-negotiation-schema.sql");
    }

    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private ContractNegotiationStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements(clock);
    }
}
