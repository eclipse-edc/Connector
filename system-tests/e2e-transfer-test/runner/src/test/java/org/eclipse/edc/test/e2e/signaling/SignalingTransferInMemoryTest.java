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

package org.eclipse.edc.test.e2e.signaling;

import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EdcClassRuntimesExtension;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;

@EndToEndTest
class SignalingTransferInMemoryTest extends AbstractSignalingTransfer {

    static String[] controlPlaneModules = new String[]{
            ":system-tests:e2e-transfer-test:control-plane",
            ":extensions:control-plane:transfer:transfer-data-plane-signaling",
            ":extensions:control-plane:callback:callback-event-dispatcher",
            ":extensions:control-plane:callback:callback-http-dispatcher",
            ":extensions:data-plane:data-plane-signaling:data-plane-signaling-client"
    };

    static String[] dataPlanePostgresqlModules = new String[]{
            ":system-tests:e2e-transfer-test:data-plane",
            ":extensions:data-plane:data-plane-public-api-v2"
    };

    static EdcRuntimeExtension dataPlane = new EdcRuntimeExtension(
            "provider-data-plane",
            PROVIDER.dataPlaneConfiguration(),
            dataPlanePostgresqlModules
    );

    @RegisterExtension
    static EdcClassRuntimesExtension runtimes = new EdcClassRuntimesExtension(
            new EdcRuntimeExtension(
                    "consumer-control-plane",
                    CONSUMER.controlPlaneConfiguration(),
                    controlPlaneModules
            ),
            new EdcRuntimeExtension(
                    ":system-tests:e2e-transfer-test:backend-service",
                    "consumer-backend-service",
                    new HashMap<>() {
                        {
                            put("web.http.port", String.valueOf(CONSUMER.backendService().getPort()));
                        }
                    }
            ),
            dataPlane,
            new EdcRuntimeExtension(
                    "provider-control-plane",
                    PROVIDER.controlPlaneConfiguration(),
                    controlPlaneModules
            ),
            new EdcRuntimeExtension(
                    ":system-tests:e2e-transfer-test:backend-service",
                    "provider-backend-service",
                    new HashMap<>() {
                        {
                            put("web.http.port", String.valueOf(PROVIDER.backendService().getPort()));
                        }
                    }
            )
    );

    @BeforeAll
    static void setup() {
        var generator = dataPlane.getContext().getService(PublicEndpointGeneratorService.class);
        generator.addGeneratorFunction("HttpData", dataAddress -> Endpoint.url(PROVIDER.publicDataPlane() + "/v2/"));
    }
}
