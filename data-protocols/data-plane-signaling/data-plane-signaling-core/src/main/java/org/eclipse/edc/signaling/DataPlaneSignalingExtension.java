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

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.signaling.logic.authorization.SignalingAuthorizationRegistryImpl;
import org.eclipse.edc.signaling.port.ClientFactory;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;

import static org.eclipse.edc.signaling.DataPlaneSignalingExtension.NAME;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(NAME)
public class DataPlaneSignalingExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Signaling";

    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private TypeManager typeManager;
    @Inject
    private Monitor monitor;

    private final SignalingAuthorizationRegistry signalingAuthorizationRegistry = new SignalingAuthorizationRegistryImpl();

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public SignalingAuthorizationRegistry signalingAuthorizationRegistry() {
        return signalingAuthorizationRegistry;
    }

    @Provider
    public ClientFactory clientFactory() {
        return new ClientFactory(httpClient, () -> typeManager.getMapper(JSON_LD), signalingAuthorizationRegistry);
    }

    @Override
    public void start() {
        if (signalingAuthorizationRegistry.getAll().isEmpty()) {
            monitor.warning("No Signaling Authorization profiles are supported: communication with Data Plane will be un-authorized.");
        }
    }
}
