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

package org.eclipse.edc.test.e2e.versionapi;

import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;

public interface Runtimes {

    static RuntimeExtension inMemoryRuntime() {
        var rt = new RuntimePerClassExtension(new EmbeddedRuntime("control-plane", inMemoryConfiguration(), ":system-tests:version-api:version-api-test-runtime"));
        rt.registerServiceMock(DataPlaneManager.class, mock());
        rt.registerServiceMock(DataPlaneClientFactory.class, mock());
        return rt;
    }


    @NotNull
    static HashMap<String, String> inMemoryConfiguration() {
        return new HashMap<>() {
            {
                put("web.http.path", "/");
                put("web.http.protocol.path", "/protocol");
                var dspPort = getFreePort();
                put("web.http.protocol.port", String.valueOf(dspPort));
                put("edc.dsp.callback.address", "http://localhost:" + dspPort + "/protocol");
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.control.port", String.valueOf(getFreePort()));
                put("web.http.management.path", "/management");
                put("web.http.management.port", String.valueOf(8181));
            }
        };
    }

}
