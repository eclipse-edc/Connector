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

package org.eclipse.edc.sql.bootstrapper;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapperExtension.NAME;

@Extension(value = NAME, categories = { "sql", "persistence", "storage" })
public class SqlSchemaBootstrapperExtension implements ServiceExtension {
    public static final String NAME = "SQL Schema Bootstrapper Extension";
    public static final String SCHEMA_AUTOCREATE_PROPERTY = "edc.sql.schema.autocreate";
    public static final boolean SCHEMA_AUTOCREATE_DEFAULT = false;

    @Inject
    private TransactionContext transactionContext;
    @Inject
    private QueryExecutor queryExecutor;
    @Inject
    private DataSourceRegistry datasourceRegistry;

    private SqlSchemaBootstrapper bootstrapper;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var shouldAutoCreate = context.getConfig().getBoolean(SCHEMA_AUTOCREATE_PROPERTY, SCHEMA_AUTOCREATE_DEFAULT);
        if (shouldAutoCreate) {
            bootstrapper = new SqlSchemaBootstrapper(transactionContext, queryExecutor, datasourceRegistry);
        }
    }

    @Override
    public void prepare() {
        if (bootstrapper != null) {
            bootstrapper.executeSql().orElseThrow(f -> new EdcPersistenceException("Failed to bootstrap SQL schema, error '%s'".formatted(f.getFailureDetail())));
        }
    }
}
