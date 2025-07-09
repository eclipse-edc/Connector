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

package org.eclipse.edc.connector.dataplane.selector;

import org.eclipse.edc.connector.dataplane.selector.manager.DataPlaneSelectorManagerImpl;
import org.eclipse.edc.connector.dataplane.selector.service.DataPlaneSelectorServiceImpl;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.manager.DataPlaneSelectorManager;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategyRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.statemachine.StateMachineConfiguration;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Duration;

import static org.eclipse.edc.connector.dataplane.selector.DataPlaneSelectorExtension.NAME;

@Extension(NAME)
public class DataPlaneSelectorExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Selector core";

    private static final int DEFAULT_CHECK_PERIOD = 60;

    @SettingContext("edc.data.plane.selector")
    @Configuration
    private StateMachineConfiguration stateMachineConfiguration;

    @Setting(description = "the check period for data plane availability, in seconds", defaultValue = DEFAULT_CHECK_PERIOD + "", key = "edc.data.plane.selector.state-machine.check.period")
    private int selectorCheckPeriod;

    @Inject
    private DataPlaneInstanceStore instanceStore;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private SelectionStrategyRegistry selectionStrategyRegistry;
    @Inject
    private DataPlaneClientFactory clientFactory;

    private DataPlaneSelectorManager manager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var configuration = new DataPlaneSelectorManagerConfiguration(
                stateMachineConfiguration.iterationWaitExponentialWaitStrategy(),
                stateMachineConfiguration.batchSize(),
                Duration.ofSeconds(selectorCheckPeriod)
        );

        manager = DataPlaneSelectorManagerImpl.Builder.newInstance()
                .clientFactory(clientFactory)
                .store(instanceStore)
                .monitor(context.getMonitor())
                .configuration(configuration)
                .entityRetryProcessConfiguration(stateMachineConfiguration.entityRetryProcessConfiguration())
                .build();
    }

    @Override
    public void start() {
        manager.start();
    }

    @Override
    public void shutdown() {
        if (manager != null) {
            manager.stop();
        }
    }

    @Provider
    public DataPlaneSelectorService dataPlaneSelectorService() {
        return new DataPlaneSelectorServiceImpl(instanceStore, selectionStrategyRegistry, transactionContext);
    }

}
