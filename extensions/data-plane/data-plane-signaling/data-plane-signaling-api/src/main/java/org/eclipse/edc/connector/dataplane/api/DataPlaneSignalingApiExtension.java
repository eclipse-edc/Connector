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

import org.eclipse.edc.connector.api.signaling.configuration.SignalingApiConfiguration;
import org.eclipse.edc.connector.dataplane.api.controller.v1.DataPlaneSignalingApiController;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.connector.dataplane.api.DataPlaneSignalingApiExtension.NAME;

@Extension(NAME)
public class DataPlaneSignalingApiExtension implements ServiceExtension {
    public static final String NAME = "DataPlane Signaling API extension";

    @Inject
    private WebService webService;

    @Inject
    private SignalingApiConfiguration controlApiConfiguration;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(controlApiConfiguration.getContextAlias(), new DataPlaneSignalingApiController());
    }
}
