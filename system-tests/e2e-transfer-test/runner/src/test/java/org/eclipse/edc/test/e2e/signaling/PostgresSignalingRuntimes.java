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

import org.eclipse.edc.junit.extensions.EdcClassRuntimesExtension;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;

import static org.eclipse.edc.test.e2e.signaling.SignalingEndToEndTestBase.CONSUMER;
import static org.eclipse.edc.test.e2e.signaling.SignalingEndToEndTestBase.PROVIDER;

public interface PostgresSignalingRuntimes {

    String[] CONTROL_PLANE_MODULES = new String[]{
            ":system-tests:e2e-transfer-test:control-plane",
            ":core:common:edr-store-core",
            ":extensions:common:store:sql:edr-index-sql",
            ":extensions:common:sql:sql-pool:sql-pool-apache-commons",
            ":extensions:common:transaction:transaction-local",
            ":extensions:control-plane:transfer:transfer-data-plane-signaling",
            ":extensions:control-plane:api:management-api:edr-cache-api",
            ":extensions:control-plane:edr:edr-store-receiver",
            ":extensions:data-plane:data-plane-signaling:data-plane-signaling-client",
            ":extensions:control-plane:callback:callback-event-dispatcher",
            ":extensions:control-plane:callback:callback-http-dispatcher"
    };

    String[] DATA_PLANE_MODULES = new String[]{
            ":system-tests:e2e-transfer-test:data-plane",
            ":extensions:data-plane:data-plane-public-api-v2"
    };

    EdcRuntimeExtension DATA_PLANE = new EdcRuntimeExtension(
            "provider-data-plane",
            PROVIDER.dataPlaneConfiguration(),
            DATA_PLANE_MODULES
    );

    @RegisterExtension
    EdcClassRuntimesExtension RUNTIMES = new EdcClassRuntimesExtension(
            new EdcRuntimeExtension(
                    "consumer-control-plane",
                    CONSUMER.controlPlanePostgresConfiguration(),
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
            DATA_PLANE,
            new EdcRuntimeExtension(
                    "provider-control-plane",
                    PROVIDER.controlPlanePostgresConfiguration(),
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
