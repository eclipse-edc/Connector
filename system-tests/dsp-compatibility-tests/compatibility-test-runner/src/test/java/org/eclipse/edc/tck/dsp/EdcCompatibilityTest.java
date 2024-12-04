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

package org.eclipse.edc.tck.dsp;

import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public class EdcCompatibilityTest {

    @RegisterExtension
    protected RuntimeExtension runtime =
            new RuntimePerClassExtension(new EmbeddedRuntime("CUnT",
                    new HashMap<>() {
                        {
                            put("edc.participant.id", "CONNECTOR_UNDER_TEST");
                            put("web.http.port", "8080");
                            put("web.http.path", "/api");
                            put("web.http.version.port", String.valueOf(getFreePort()));
                            put("web.http.version.path", "/api/version");
                            put("web.http.control.port", String.valueOf(getFreePort()));
                            put("web.http.control.path", "/api/control");
                            put("web.http.management.port", "8081");
                            put("web.http.management.path", "/api/management");
                            put("web.http.protocol.port", "8282");
                            put("web.http.protocol.path", "/api/v1/dsp"); // expected by TCK
                            put("web.api.auth.key", "password");
                            put("edc.dsp.callback.address", "http://localhost:8282/api/v1/dsp");
                            put("edc.management.context.enabled", "true");
                        }
                    },
                    ":system-tests:dsp-compatibility-tests:connector-under-test"
            ));

    @Test
    void assertRuntimeReady() throws InterruptedException {
        var l = new CountDownLatch(1);
        l.await();
    }
}

