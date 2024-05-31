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

import org.eclipse.edc.api.auth.spi.ControlClientAuthenticationProvider;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.Objects;

/**
 * This extension provides the Data Plane API:
 * - Control API: set of endpoints to trigger/monitor/cancel data transfers that should be accessible only from the Control Plane.
 * - Public API: generic endpoint open to other participants of the Dataspace and used to proxy a data request to the actual data source.
 *
 * @deprecated replaced by data-plane-signaling.
 */
@Extension(value = DataPlaneClientExtension.NAME)
@Deprecated(since = "0.6.0")
public class DataPlaneClientExtension implements ServiceExtension {
    public static final String NAME = "DEPRECATED: Data Plane Client";

    @Inject(required = false)
    private DataPlaneManager dataPlaneManager;
    @Inject(required = false)
    private EdcHttpClient httpClient;
    @Inject
    private TypeManager typeManager;
    @Inject
    private ControlClientAuthenticationProvider authenticationProvider;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DataPlaneClientFactory dataPlaneClientFactory(ServiceExtensionContext context) {
        context.getMonitor().warning("the `data-plane-client` extension has been deprecated, please switch to data-plane-signaling");
        if (dataPlaneManager != null) {
            // Data plane manager is embedded in the current runtime
            context.getMonitor().debug(() -> "Using embedded Data Plane client.");
            return instance -> new EmbeddedDataPlaneClient(dataPlaneManager);
        }

        context.getMonitor().debug(() -> "Using remote Data Plane client.");
        Objects.requireNonNull(httpClient, "To use remote Data Plane client, an EdcHttpClient instance must be registered");
        return instance -> new RemoteDataPlaneClient(httpClient, typeManager.getMapper(), instance, authenticationProvider);
    }
}


