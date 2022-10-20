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

package org.eclipse.edc.connector.dataplane.transfer;

import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.transfer.client.EmbeddedDataPlaneTransferClient;
import org.eclipse.edc.connector.dataplane.transfer.client.RemoteDataPlaneTransferClient;
import org.eclipse.edc.connector.dataplane.transfer.flow.DataPlaneTransferFlowController;
import org.eclipse.edc.connector.dataplane.transfer.spi.client.DataPlaneTransferClient;
import org.eclipse.edc.connector.transfer.spi.callback.ControlPlaneApiUrl;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Objects;

import static java.lang.String.format;

/**
 * Provides client to delegate data transfer to Data Plane.
 */
@Extension(value = DataPlaneTransferClientExtension.NAME)
public class DataPlaneTransferClientExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Transfer Client";
    @Setting
    private static final String DPF_SELECTOR_STRATEGY = "edc.transfer.client.selector.strategy";
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

    @Inject(required = false)
    private ControlPlaneApiUrl callbackUrl;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        DataPlaneTransferClient client;
        if (dataPlaneManager != null) {
            // Data plane manager is embedded in the current runtime
            client = new EmbeddedDataPlaneTransferClient(dataPlaneManager);
            monitor.debug(() -> "Using embedded Data Plane.");
        } else {
            Objects.requireNonNull(okHttpClient, "If no DataPlaneManager is embedded, a OkHttpClient instance must be provided");
            Objects.requireNonNull(retryPolicy, "If no DataPlaneManager is embedded, a RetryPolicy instance must be provided");
            Objects.requireNonNull(selectorClient, "If no DataPlaneManager is embedded, a DataPlaneSelector instance must be provided");
            var selectionStrategy = context.getSetting(DPF_SELECTOR_STRATEGY, "random");
            client = new RemoteDataPlaneTransferClient(okHttpClient, selectorClient, selectionStrategy, retryPolicy, context.getTypeManager().getMapper());
            monitor.debug(() -> format("Using remote Data Plane with selectionStratey=%s.", selectionStrategy));
        }

        var flowController = new DataPlaneTransferFlowController(client, callbackUrl);
        flowManager.register(flowController);
    }
}
