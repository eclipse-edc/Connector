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

package org.eclipse.edc.iam.identitytrust.sts.store;


import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.iam.identitytrust.sts.store.schema.StsClientStatements;
import org.eclipse.edc.iam.identitytrust.sts.store.schema.postgres.PostgresDialectStatements;
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

import static org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry.DEFAULT_DATASOURCE;

@Provides({ StsAccountStore.class })
@Extension(value = "SQL sts accounts store")
public class SqlStsClientStoreExtension implements ServiceExtension {

    @Setting(description = "The datasource to be used", defaultValue = DEFAULT_DATASOURCE, key = "edc.sql.store.stsclient.datasource")
    private String dataSourceName;

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject(required = false)
    private StsClientStatements statements;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private TypeManager typeManager;

    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Override
    public void initialize(ServiceExtensionContext context) {

        var sqlStore = new SqlStsAccountStore(dataSourceRegistry, dataSourceName, transactionContext, typeManager.getMapper(),
                getStatementImpl(), queryExecutor);

        context.registerService(StsAccountStore.class, sqlStore);

        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "sts-client-schema.sql");
    }

    private StsClientStatements getStatementImpl() {
        return statements == null ? new PostgresDialectStatements() : statements;
    }

}
