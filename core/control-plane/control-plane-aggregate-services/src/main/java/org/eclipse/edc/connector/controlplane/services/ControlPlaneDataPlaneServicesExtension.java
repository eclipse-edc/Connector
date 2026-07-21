/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.services;

import org.eclipse.edc.connector.controlplane.dataplane.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.controlplane.dataplane.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.controlplane.dataplane.spi.strategy.SelectionStrategyRegistry;
import org.eclipse.edc.connector.controlplane.services.dataplane.DataPlaneSelectorServiceImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

@Extension(ControlPlaneDataPlaneServicesExtension.NAME)
public class ControlPlaneDataPlaneServicesExtension implements ServiceExtension {

    public static final String NAME = "Control Plane Data Plane Services";

    @Setting(
            description = "Defines strategy for Data Plane instance selection in case Data Plane is not embedded in current runtime",
            defaultValue = "random",
            key = "edc.dataplane.client.selector.strategy"
    )
    private String selectionStrategy;

    @Inject
    private DataPlaneInstanceStore instanceStore;
    @Inject
    private SelectionStrategyRegistry selectionStrategyRegistry;
    @Inject
    private TransactionContext transactionContext;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DataPlaneSelectorService dataPlaneSelectorService() {
        return new DataPlaneSelectorServiceImpl(instanceStore, selectionStrategyRegistry, transactionContext, selectionStrategy);
    }
}
