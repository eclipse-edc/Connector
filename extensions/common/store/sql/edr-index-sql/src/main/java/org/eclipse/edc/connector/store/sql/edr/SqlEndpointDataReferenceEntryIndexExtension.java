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

package org.eclipse.edc.connector.store.sql.edr;


import org.eclipse.edc.connector.store.sql.edr.schema.EndpointDataReferenceEntryStatements;
import org.eclipse.edc.connector.store.sql.edr.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

@Provides({ EndpointDataReferenceEntryIndex.class })
@Extension(value = "SQL edr entry store")
public class SqlEndpointDataReferenceEntryIndexExtension implements ServiceExtension {

    /**
     * Name of the datasource to use for accessing edr entries.
     */
    @Setting(required = true)
    public static final String DATASOURCE_SETTING_NAME = "edc.datasource.edr.name";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject(required = false)
    private EndpointDataReferenceEntryStatements statements;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var dataSourceName = context.getConfig().getString(DATASOURCE_SETTING_NAME, DataSourceRegistry.DEFAULT_DATASOURCE);

        var sqlStore = new SqlEndpointDataReferenceEntryIndex(dataSourceRegistry, dataSourceName, transactionContext, typeManager.getMapper(),
                getStatementImpl(), queryExecutor);

        context.registerService(EndpointDataReferenceEntryIndex.class, sqlStore);
    }

    private EndpointDataReferenceEntryStatements getStatementImpl() {
        return statements == null ? new PostgresDialectStatements() : statements;
    }

}
