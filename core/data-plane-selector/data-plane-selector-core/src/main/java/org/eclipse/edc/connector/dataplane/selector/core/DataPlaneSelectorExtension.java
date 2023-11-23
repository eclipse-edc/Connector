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
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategyRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

@Extension(value = "DataPlane core selector")
public class DataPlaneSelectorExtension implements ServiceExtension {

    @Inject
    private DataPlaneInstanceStore instanceStore;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private SelectionStrategyRegistry selectionStrategyRegistry;

    @Provider
    public DataPlaneSelectorService dataPlaneSelectorService() {
        var selector = new DataPlaneSelectorImpl(instanceStore);
        return new EmbeddedDataPlaneSelectorService(selector, instanceStore, selectionStrategyRegistry, transactionContext);
    }

}
