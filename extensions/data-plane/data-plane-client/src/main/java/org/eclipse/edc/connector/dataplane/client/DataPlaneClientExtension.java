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

package org.eclipse.edc.connector.dataplane.client;

import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * This extension provides the Data Plane API:
 * - Control API: set of endpoints to trigger/monitor/cancel data transfers that should be accessible only from the Control Plane.
 * - Public API: generic endpoint open to other participants of the Dataspace and used to proxy a data request to the actual data source.
 */
@Extension(value = DataPlaneClientExtension.NAME)
public class DataPlaneClientExtension implements ServiceExtension {
    public static final String NAME = "Data Plane Client";

    @Setting(value = "Defines strategy for Data Plane instance selection in case Data Plane is not embedded in current runtime")
    private static final String DPF_SELECTOR_STRATEGY = "edc.dataplane.client.selector.strategy";

    @Inject(required = false)
    private DataPlaneManager dataPlaneManager;

    @Inject(required = false)
    private DataPlaneSelectorClient dataPlaneSelectorClient;

    @Inject(required = false)
    private OkHttpClient httpClient;

    @Inject(required = false)
    private RetryPolicy<Object> retryPolicy;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DataPlaneClient dataPlaneClient(ServiceExtensionContext context) {
        if (dataPlaneManager != null) {
            // Data plane manager is embedded in the current runtime
            context.getMonitor().debug(() -> "Using embedded Data Plane client.");
            return new EmbeddedDataPlaneClient(dataPlaneManager);
        }

        context.getMonitor().debug(() -> "Using remote Data Plane client.");
        var selectionStrategy = context.getSetting(DPF_SELECTOR_STRATEGY, "random");
        return new RemoteDataPlaneClient(httpClient, dataPlaneSelectorClient, selectionStrategy, retryPolicy, context.getTypeManager().getMapper());
    }
}


