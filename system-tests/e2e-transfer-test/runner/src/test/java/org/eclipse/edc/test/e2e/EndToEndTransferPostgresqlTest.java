/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import org.eclipse.edc.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.edc.junit.extensions.EdcClassRuntimesExtension;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;

import static org.eclipse.edc.test.e2e.PostgresUtil.createDatabase;

@PostgresqlDbIntegrationTest
class EndToEndTransferPostgresqlTest extends AbstractEndToEndTransfer {

    @RegisterExtension
    static BeforeAllCallback createDatabase = context -> {
        createDatabase(CONSUMER);
        createDatabase(PROVIDER);
    };

    @RegisterExtension
    static EdcClassRuntimesExtension runtimes = new EdcClassRuntimesExtension(
            new EdcRuntimeExtension(
                    ":system-tests:e2e-transfer-test:control-plane-postgresql",
                    "consumer-control-plane",
                    CONSUMER.controlPlanePostgresConfiguration()
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
                    ":system-tests:e2e-transfer-test:data-plane",
                    "provider-data-plane",
                    PROVIDER.dataPlanePostgresConfiguration()
            ),
            new EdcRuntimeExtension(
                    ":system-tests:e2e-transfer-test:control-plane-postgresql",
                    "provider-control-plane",
                    PROVIDER.controlPlanePostgresConfiguration()
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
