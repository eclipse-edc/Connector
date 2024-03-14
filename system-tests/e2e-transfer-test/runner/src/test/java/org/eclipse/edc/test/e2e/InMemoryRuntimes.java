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

package org.eclipse.edc.test.e2e;

import org.eclipse.edc.junit.extensions.EdcClassRuntimesExtension;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;

import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.CONSUMER;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.PROVIDER;

public interface InMemoryRuntimes {

    String[] CONTROL_PLANE_MODULES = new String[]{
            ":system-tests:e2e-transfer-test:control-plane",
            ":extensions:control-plane:transfer:transfer-data-plane",
            ":extensions:data-plane:data-plane-client"
    };

    @RegisterExtension
    EdcClassRuntimesExtension RUNTIMES = new EdcClassRuntimesExtension(
            new EdcRuntimeExtension(
                    "consumer-control-plane",
                    CONSUMER.controlPlaneConfiguration(),
                    CONTROL_PLANE_MODULES
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
            new EdcRuntimeExtension(
                    "provider-data-plane",
                    PROVIDER.dataPlaneConfiguration(),
                    ":system-tests:e2e-transfer-test:data-plane",
                    ":extensions:data-plane:data-plane-public-api"),
            new EdcRuntimeExtension(
                    "provider-control-plane",
                    PROVIDER.controlPlaneConfiguration(),
                    CONTROL_PLANE_MODULES
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

}
