/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.policy.monitor.store.sql;

import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.connector.policy.monitor.store.sql.schema.PolicyMonitorStatements;
import org.eclipse.edc.connector.policy.monitor.store.sql.schema.PostgresPolicyMonitorStatements;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper;
import org.eclipse.edc.sql.configuration.DataSourceName;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

import static org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry.DEFAULT_DATASOURCE;

public class SqlPolicyMonitorStoreExtension implements ServiceExtension {

    @Deprecated(since = "0.8.1")
    @Setting(value = "Name of the datasource to use for accessing policy monitor store", defaultValue = DEFAULT_DATASOURCE)
    private static final String DATASOURCE_SETTING_NAME = "edc.datasource.policy-monitor.name";

    @Setting(value = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE)
    public static final String DATASOURCE_NAME = "edc.sql.store.policy-monitor.datasource";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject(required = false)
    private PolicyMonitorStatements statements;

    @Inject
    private Clock clock;

    @Inject
    private TypeManager typeManager;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Provider
    public PolicyMonitorStore policyMonitorStore(ServiceExtensionContext context) {
        var dataSourceName = DataSourceName.getDataSourceName(DATASOURCE_NAME, DATASOURCE_SETTING_NAME, context.getConfig(), context.getMonitor());

        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "policy-monitor-schema.sql");

        return new SqlPolicyMonitorStore(dataSourceRegistry, dataSourceName, transactionContext,
                getStatementImpl(), typeManager.getMapper(), clock, queryExecutor, context.getRuntimeId());

    }

    /**
     * returns an externally-provided sql statement dialect, or postgres as a default
     */
    private PolicyMonitorStatements getStatementImpl() {
        return statements != null ? statements : new PostgresPolicyMonitorStatements();
    }

}
