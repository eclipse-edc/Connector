/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataplane.selector.client.DataPlaneSelectorClient;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.transfer.dataplane.client.DataPlaneTransferClient;
import org.eclipse.dataspaceconnector.transfer.dataplane.client.EmbeddedDataPlaneTransferClient;
import org.eclipse.dataspaceconnector.transfer.dataplane.client.RemoteDataPlaneTransferClient;
import org.eclipse.dataspaceconnector.transfer.dataplane.flow.DataPlaneTransferFlowController;

import java.util.Objects;

/**
 * Provides client to delegate data transfer to Data Plane.
 */
public class DataPlaneTransferClientExtension implements ServiceExtension {

    @EdcSetting
    private static final String DPF_SELECTOR_STRATEGY = "edc.dpf.selector.strategy";

    @Inject(required = false)
    private DataPlaneSelectorClient selectorClient;

    @Inject(required = false)
    private DataPlaneManager dataPlaneManager;

    @Inject(required = false)
    private OkHttpClient okHttpClient;

    @Inject(required = false)
    private RetryPolicy<Object> retryPolicy;

    @Inject
    private DataAddressResolver addressResolver;

    @Inject
    private DataFlowManager flowManager;

    @Override
    public String name() {
        return "Data Plane Transfer Client";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        DataPlaneTransferClient client;
        if (dataPlaneManager != null) {
            // Data plane manager is embedded in the current runtime
            client = new EmbeddedDataPlaneTransferClient(dataPlaneManager);
        } else {
            Objects.requireNonNull(okHttpClient, "If no DataPlaneManager is embedded, a OkHttpClient instance must be provided");
            Objects.requireNonNull(retryPolicy, "If no DataPlaneManager is embedded, a RetryPolicy instance must be provided");
            Objects.requireNonNull(selectorClient, "If no DataPlaneManager is embedded, a DataPlaneSelector instance must be provided");
            var selectionStrategy = context.getSetting(DPF_SELECTOR_STRATEGY, "random");
            client = new RemoteDataPlaneTransferClient(okHttpClient, selectorClient, selectionStrategy, retryPolicy, context.getTypeManager().getMapper());
        }

        var flowController = new DataPlaneTransferFlowController(addressResolver, client);
        flowManager.register(flowController);
    }
}
