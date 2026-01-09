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

package org.eclipse.edc.connector.controlplane.api.client;

import org.eclipse.edc.connector.controlplane.api.client.transferprocess.TransferProcessHttpClient;
import org.eclipse.edc.connector.controlplane.api.client.transferprocess.model.TransferProcessFailRequest;
import org.eclipse.edc.connector.dataplane.spi.port.TransferProcessApiClient;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

/**
 * Extensions that contains clients for Control Plane HTTP APIs
 */
@Extension(value = ControlPlaneApiClientExtension.NAME)
public class ControlPlaneApiClientExtension implements ServiceExtension {

    public static final String NAME = "Control Plane HTTP API client";

    @Inject
    private TypeManager typeManager;
    @Inject
    private ControlApiHttpClient httpClient;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public TransferProcessApiClient transferProcessApiClient(ServiceExtensionContext context) {
        typeManager.registerTypes(TransferProcessFailRequest.class);

        return new TransferProcessHttpClient(httpClient, typeManager.getMapper(), context.getMonitor());
    }

}
