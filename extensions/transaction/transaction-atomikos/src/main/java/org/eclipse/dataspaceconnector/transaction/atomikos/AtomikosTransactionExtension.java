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
package org.eclipse.dataspaceconnector.transaction.atomikos;

import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.dataspaceconnector.transaction.atomikos.DataSourceConfigurationParser.parseDataSourceConfigurations;
import static org.eclipse.dataspaceconnector.transaction.atomikos.Setters.setIfProvided;
import static org.eclipse.dataspaceconnector.transaction.atomikos.Setters.setIfProvidedInt;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.CHECKPOINT_INTERVAL;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.DATA_DIR;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.LOGGING;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.THREADED2PC;
import static org.eclipse.dataspaceconnector.transaction.atomikos.TransactionManagerConfigurationKeys.TIMEOUT;


/**
 * Provides an implementation of a {@link DataSourceRegistry} and a {@link TransactionContext} backed by Atomikos.
 */
@Provides({TransactionContext.class, DataSourceRegistry.class})
public class AtomikosTransactionExtension implements ServiceExtension {
    static final String EDC_DATASOURCE_PREFIX = "edc.datasource";
    private AtomikosTransactionPlatform transactionPlatform;
    private AtomikosTransactionContext transactionContext;

    @Override
    public String name() {
        return "Atomikos Transaction";
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
