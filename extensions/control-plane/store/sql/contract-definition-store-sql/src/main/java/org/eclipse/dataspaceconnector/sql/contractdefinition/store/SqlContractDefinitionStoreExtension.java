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
 *       Microsoft Corporation - refactoring
 *
 */

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;


import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Setting;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema.ContractDefinitionStatements;
import org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema.postgres.PostgresDialectStatements;

@Provides({ ContractDefinitionStore.class })
@Extension(value = "SQL contract definition store")
public class SqlContractDefinitionStoreExtension implements ServiceExtension {

    /**
     * Name of the datasource to use for accessing contract definitions.
     */
    @Setting(required = true)
    private static final String DATASOURCE_SETTING_NAME = "edc.datasource.contractdefinition.name";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject(required = false)
    private ContractDefinitionStatements statements;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var dataSourceName = context.getConfig().getString(DATASOURCE_SETTING_NAME);

        var sqlContractDefinitionStore = new SqlContractDefinitionStore(dataSourceRegistry, dataSourceName, transactionContext, getStatementImpl(), context.getTypeManager());

        context.registerService(ContractDefinitionStore.class, sqlContractDefinitionStore);
    }

    private ContractDefinitionStatements getStatementImpl() {
        return statements == null ? new PostgresDialectStatements() : statements;
    }

}
