/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.store.sql.participantcontext;

import org.eclipse.edc.connector.store.sql.participantcontext.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
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

import static org.eclipse.edc.connector.store.sql.participantcontext.SqlParticipantContextStoreExtension.NAME;

@Extension(value = NAME)
public class SqlParticipantContextStoreExtension implements ServiceExtension {
    public static final String NAME = "ParticipantContext SQL Store Extension";

    @Setting(description = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.participantcontext.datasource")
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
    private ParticipantContextStoreStatements statements;
    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Override
    public void initialize(ServiceExtensionContext context) {
        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "participant-context-schema.sql");
    }

    @Provider
    public ParticipantContextStore createSqlStore(ServiceExtensionContext context) {
        return new SqlParticipantContextStore(dataSourceRegistry, dataSourceName, transactionContext, typemanager.getMapper(),
                queryExecutor, getStatementImpl());
    }

    private ParticipantContextStoreStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements();
    }

}
