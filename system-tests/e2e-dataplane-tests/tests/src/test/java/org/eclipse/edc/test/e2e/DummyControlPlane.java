/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e;

import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockserver.integration.ClientAndServer;

import java.util.Map;
import java.util.function.Supplier;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class DummyControlPlane implements BeforeAllCallback, AfterAllCallback {

    private final LazySupplier<Integer> port = new LazySupplier<>(Ports::getFreePort);
    private ClientAndServer dummyControlPlane;

    public Supplier<Config> dataPlaneConfigurationSupplier() {
        return () -> ConfigFactory.fromMap(Map.of(
                "edc.dpf.selector.url", "http://localhost:" + port.get()
        ));
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        dummyControlPlane = ClientAndServer.startClientAndServer(port.get());
        dummyControlPlane.when(request()).respond(response());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (dummyControlPlane != null) {
            dummyControlPlane.stop();
        }
    }
}
