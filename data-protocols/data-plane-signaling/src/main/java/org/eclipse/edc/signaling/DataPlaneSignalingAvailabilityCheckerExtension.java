/*
 *  Copyright (c) 2025 Think-it GmbH
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

package org.eclipse.edc.signaling;

import org.eclipse.edc.connector.dataplane.selector.spi.manager.DataPlaneAvailabilityChecker;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.signaling.port.ClientFactory;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;

import static org.eclipse.edc.signaling.DataPlaneSignalingAvailabilityCheckerExtension.NAME;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(NAME)
public class DataPlaneSignalingAvailabilityCheckerExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Signaling Availability Checker";

    @Inject
    private ControlApiHttpClient httpClient;
    @Inject
    private TypeManager typeManager;

    private ClientFactory clientFactory;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DataPlaneAvailabilityChecker dataPlaneAvailabilityChecker() {
        var clientFactory = clientFactory();
        return dataPlane -> clientFactory.createClient(dataPlane).checkAvailability();
    }

    @Provider
    public ClientFactory clientFactory() {
        if (clientFactory == null) {
            clientFactory = new ClientFactory(httpClient, () -> typeManager.getMapper(JSON_LD));
        }
        return clientFactory;
    }

}
