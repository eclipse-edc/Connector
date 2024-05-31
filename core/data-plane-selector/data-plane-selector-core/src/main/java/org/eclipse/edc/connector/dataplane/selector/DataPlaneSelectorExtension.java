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

import org.eclipse.edc.connector.dataplane.selector.manager.DataPlaneInstanceManagerImpl;
import org.eclipse.edc.connector.dataplane.selector.service.EmbeddedDataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.manager.DataPlaneInstanceManager;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategyRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

@Extension(value = "DataPlane core selector")
public class DataPlaneSelectorExtension implements ServiceExtension {

    @Inject
    private DataPlaneInstanceStore instanceStore;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private SelectionStrategyRegistry selectionStrategyRegistry;

    private DataPlaneInstanceManager manager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        manager = DataPlaneInstanceManagerImpl.Builder.newInstance()
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
        return new EmbeddedDataPlaneSelectorService(instanceStore, selectionStrategyRegistry, transactionContext);
    }

}
