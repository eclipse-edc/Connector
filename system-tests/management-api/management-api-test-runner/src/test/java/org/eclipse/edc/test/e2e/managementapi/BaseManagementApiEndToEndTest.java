/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.test.e2e.managementapi;

import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.UUID;

import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;

public abstract class BaseManagementApiEndToEndTest {

    public static final int PORT = getFreePort();
    public static final int PROTOCOL_PORT = getFreePort();

    @RegisterExtension
    static EdcRuntimeExtension controlPlane = new EdcRuntimeExtension(
            ":system-tests:management-api:management-api-test-runtime",
            "control-plane",
            new HashMap<>() {
                {
                    put("edc.ids.id", "urn:connector:" + UUID.randomUUID());
                    put("web.http.path", "/");
                    put("web.http.protocol.path", "/protocol");
                    put("web.http.protocol.port", String.valueOf(PROTOCOL_PORT));
                    put("edc.dsp.callback.address", "http://localhost:" + PROTOCOL_PORT + "/protocol");
                    put("web.http.port", String.valueOf(getFreePort()));
                    put("web.http.management.path", "/management");
                    put("web.http.management.port", String.valueOf(PORT));
                }
            }
    );
}
