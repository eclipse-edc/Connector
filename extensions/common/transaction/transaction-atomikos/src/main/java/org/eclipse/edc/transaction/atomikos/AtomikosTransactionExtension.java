/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.transaction.atomikos;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.edc.transaction.atomikos.DataSourceConfigurationParser.parseDataSourceConfigurations;
import static org.eclipse.edc.transaction.atomikos.Setters.setIfProvided;
import static org.eclipse.edc.transaction.atomikos.Setters.setIfProvidedInt;


/**
 * Provides an implementation of a {@link DataSourceRegistry} and a {@link TransactionContext} backed by Atomikos.
 */
@Provides({ TransactionContext.class, DataSourceRegistry.class })
@Extension(value = AtomikosTransactionExtension.NAME)
public class AtomikosTransactionExtension implements ServiceExtension {
    public static final String NAME = "Atomikos Transaction";

    @Setting(required = true)
    public static final String DRIVER_CLASS = "driver.class";
    @Setting(required = true)
    public static final String URL = "url";
    @Setting
    public static final String DS_TYPE = "type";
    @Setting
    public static final String USERNAME = "username";
    @Setting
    public static final String PASSWORD = "password";
    @Setting
    public static final String POOL_SIZE = "pool.size";
    @Setting
    public static final String MAX_POOL_SIZE = "max.pool.size";
    @Setting
    public static final String MIN_POOL_SIZE = "min.pool.size";
    @Setting
    public static final String CONNECTION_TIMEOUT = "connection.timeout";
    @Setting
    public static final String LOGIN_TIMEOUT = "login.timeout";
    @Setting
    public static final String MAINTENANCE_INTERVAL = "maintenance.interval";
    @Setting
    public static final String MAX_IDLE = "max.idle";
    @Setting
    public static final String QUERY = "query";
    @Setting
    public static final String DRIVER_PROPERTIES = "properties";
    @Setting(required = false)
    public static final String TIMEOUT = "edc.atomikos.timeout";
    @Setting(required = false)
    public static final String DATA_DIR = "edc.atomikos.directory";
    @Setting(required = false)
    public static final String THREADED2PC = "edc.atomikos.threaded2pc";
    @Setting(required = false)
    public static final String LOGGING = "edc.atomikos.logging";
    @Setting(required = false)
    public static final String CHECKPOINT_INTERVAL = "edc.atomikos.checkpoint.interval";
    public static final int DEFAULT_VALUE = -1;

    static final String EDC_DATASOURCE_PREFIX = "edc.datasource";
    private AtomikosTransactionPlatform transactionPlatform;
    private AtomikosTransactionContext transactionContext;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // initialize the core platform services but do not start recovery and {@link #start} in order to allow transactional resources
        // in other extensions to register themselves.
        var tmConfiguration = getTransactionManagerConfiguration(context);
        transactionPlatform = new AtomikosTransactionPlatform(tmConfiguration);
        transactionContext = new AtomikosTransactionContext(context.getMonitor());
        context.registerService(TransactionContext.class, transactionContext);

        var config = context.getConfig(EDC_DATASOURCE_PREFIX);

        var dsConfigurations = parseDataSourceConfigurations(config);
        var dataSourceRegistry = new AtomikosDataSourceRegistry();
        dsConfigurations.forEach(dataSourceRegistry::initialize);

        context.registerService(DataSourceRegistry.class, dataSourceRegistry);
    }

    @Override
    public void start() {
        // recover after transactional resources have initialized and registered with the platform services
        transactionPlatform.recover();
        transactionContext.initialize(transactionPlatform.getTransactionManager());
    }

    @Override
    public void shutdown() {
        if (transactionPlatform != null) {
            transactionPlatform.stop();
        }
    }

    @NotNull
    private TransactionManagerConfiguration getTransactionManagerConfiguration(ServiceExtensionContext context) {
        var builder = TransactionManagerConfiguration.Builder.newInstance();

        Config config = context.getConfig();
        var name = context.getConnectorId().replace(":", "_");
        builder.name(name);

        setIfProvidedInt(CHECKPOINT_INTERVAL, builder::checkPointInterval, config);
        Setters.setIfProvidedInt(TIMEOUT, builder::timeout, config);
        setIfProvided(DATA_DIR, builder::dataDir, config);
        setIfProvided(LOGGING, (val) -> builder.enableLogging(Boolean.parseBoolean(val)), config);
        setIfProvided(THREADED2PC, (val) -> builder.singleThreaded2Pc(Boolean.parseBoolean(val)), config);

        return builder.build();
    }

}
