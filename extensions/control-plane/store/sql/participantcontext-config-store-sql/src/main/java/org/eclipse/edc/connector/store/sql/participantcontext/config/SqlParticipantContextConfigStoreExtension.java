/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.store.sql.participantcontext.config;

import org.eclipse.edc.connector.store.sql.participantcontext.config.schema.postgres.PostgresDialectStatementsConfig;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
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

import static org.eclipse.edc.connector.store.sql.participantcontext.config.SqlParticipantContextConfigStoreExtension.NAME;

@Extension(value = NAME)
public class SqlParticipantContextConfigStoreExtension implements ServiceExtension {
    public static final String NAME = "ParticipantContextConfig SQL Store Extension";

    @Setting(description = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.participantcontextconfig.datasource")
    private String dataSourceName;

    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private TypeManager typemanager;
    @Inject
    private QueryExecutor queryExecutor;
    @Inject(required = false)
    private ParticipantContextConfigStoreStatements statements;
    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Override
    public void initialize(ServiceExtensionContext context) {
        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "participant-context-config-schema.sql");
    }

    @Provider
    public ParticipantContextConfigStore createSqlStore() {
        return new SqlParticipantContextConfigStore(dataSourceRegistry, dataSourceName, transactionContext, typemanager.getMapper(), queryExecutor, getStatementImpl());
    }

    private ParticipantContextConfigStoreStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatementsConfig();
    }

}
