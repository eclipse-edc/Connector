/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector.core;

import org.eclipse.edc.connector.dataplane.selector.EmbeddedDataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelector;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.RandomSelectionStrategy;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategyRegistry;
import org.eclipse.edc.connector.dataplane.selector.strategy.DefaultSelectionStrategyRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

@Provides({ DataPlaneSelector.class, SelectionStrategyRegistry.class })
@Extension(value = "DataPlane core selector")
public class DataPlaneSelectorExtension implements ServiceExtension {

    @Inject
    private DataPlaneInstanceStore instanceStore;

    @Inject
    private TransactionContext transactionContext;

    private DataPlaneSelectorService dataPlaneSelectorService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var selector = new DataPlaneSelectorImpl(instanceStore);

        var strategy = new DefaultSelectionStrategyRegistry();
        strategy.add(new RandomSelectionStrategy());

        dataPlaneSelectorService = new EmbeddedDataPlaneSelectorService(selector, instanceStore, strategy, transactionContext);

        context.registerService(DataPlaneSelector.class, selector);
        context.registerService(SelectionStrategyRegistry.class, strategy);
    }

    @Provider(isDefault = true)
    public DataPlaneSelectorService dataPlaneSelectorService() {
        return dataPlaneSelectorService;
    }

}
