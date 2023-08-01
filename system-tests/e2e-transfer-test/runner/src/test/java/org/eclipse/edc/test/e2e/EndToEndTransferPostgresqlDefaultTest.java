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
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

import static org.eclipse.edc.test.e2e.PostgresUtil.createDatabase;

@PostgresqlDbIntegrationTest
class EndToEndTransferPostgresqlDefaultTest extends AbstractEndToEndTransfer {

    @RegisterExtension
    static EdcRuntimeExtension consumerControlPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:control-plane-postgresql",
            "consumer-control-plane",
            CONSUMER.controlPlanePostgresDefaultConfiguration()
    );

    @RegisterExtension
    static EdcRuntimeExtension consumerDataPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:data-plane",
            "consumer-data-plane",
            CONSUMER.dataPlaneConfiguration()
    );

    @RegisterExtension
    static EdcRuntimeExtension consumerBackendService = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:backend-service",
            "consumer-backend-service",
            new HashMap<>() {
                {
                    put("web.http.port", String.valueOf(CONSUMER.backendService().getPort()));
                }
            }
    );

    @RegisterExtension
    static EdcRuntimeExtension providerDataPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:data-plane",
            "provider-data-plane",
            PROVIDER.dataPlaneConfiguration()
    );

    @RegisterExtension
    static EdcRuntimeExtension providerControlPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:control-plane-postgresql",
            "provider-control-plane",
            PROVIDER.controlPlanePostgresDefaultConfiguration()
    );

    @RegisterExtension
    static EdcRuntimeExtension providerBackendService = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:backend-service",
            "provider-backend-service",
            new HashMap<>() {
                {
                    put("web.http.port", String.valueOf(PROVIDER.backendService().getPort()));
                }
            }
    );

    @BeforeAll
    static void beforeAll() throws SQLException, IOException, ClassNotFoundException {
        createDatabase(CONSUMER);
        createDatabase(PROVIDER);
    }
}
