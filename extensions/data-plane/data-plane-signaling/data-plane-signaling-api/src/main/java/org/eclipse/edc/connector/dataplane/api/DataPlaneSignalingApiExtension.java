/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.api;

import org.eclipse.edc.connector.api.control.configuration.ControlApiConfiguration;
import org.eclipse.edc.connector.api.signaling.configuration.SignalingApiConfiguration;
import org.eclipse.edc.connector.dataplane.api.controller.v1.DataPlaneSignalingApiController;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.connector.dataplane.api.DataPlaneSignalingApiExtension.NAME;

@Extension(NAME)
public class DataPlaneSignalingApiExtension implements ServiceExtension {
    public static final String NAME = "DataPlane Signaling API extension";

    @Inject
    private WebService webService;
    @Inject
    private SignalingApiConfiguration signalingApiConfiguration;
    @Inject
    private ControlApiConfiguration controlApiConfiguration;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private DataPlaneManager dataPlaneManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var signalingApiTypeTransformerRegistry = transformerRegistry.forContext("signaling-api");
        var controller = new DataPlaneSignalingApiController(signalingApiTypeTransformerRegistry,
                dataPlaneManager, context.getMonitor().withPrefix("SignalingAPI"));
        webService.registerResource(signalingApiConfiguration.getContextAlias(), controller);
        webService.registerResource(controlApiConfiguration.getContextAlias(), controller);
    }
}
