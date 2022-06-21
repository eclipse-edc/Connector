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

package org.eclipse.dataspaceconnector.dataplane.selector.core;

import org.eclipse.dataspaceconnector.dataplane.selector.DataPlaneSelector;
import org.eclipse.dataspaceconnector.dataplane.selector.DataPlaneSelectorService;
import org.eclipse.dataspaceconnector.dataplane.selector.DataPlaneSelectorServiceImpl;
import org.eclipse.dataspaceconnector.dataplane.selector.store.DataPlaneInstanceStore;
import org.eclipse.dataspaceconnector.dataplane.selector.strategy.DefaultSelectionStrategyRegistry;
import org.eclipse.dataspaceconnector.dataplane.selector.strategy.RandomSelectionStrategy;
import org.eclipse.dataspaceconnector.dataplane.selector.strategy.SelectionStrategyRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides({ DataPlaneSelector.class, SelectionStrategyRegistry.class, DataPlaneSelectorService.class })
public class DataPlaneSelectorExtension implements ServiceExtension {

    @Inject
    private DataPlaneInstanceStore instanceStore;

    @Override
    public void initialize(ServiceExtensionContext context) {
        DataPlaneSelectorImpl selector = new DataPlaneSelectorImpl(instanceStore);

        DefaultSelectionStrategyRegistry strategy = new DefaultSelectionStrategyRegistry();
        strategy.add(new RandomSelectionStrategy());

        context.registerService(DataPlaneSelector.class, selector);
        context.registerService(SelectionStrategyRegistry.class, strategy);
        context.registerService(DataPlaneSelectorService.class, new DataPlaneSelectorServiceImpl(selector, instanceStore, strategy));
    }
}
