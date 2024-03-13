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
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;

import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.createDatabase;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.CONSUMER;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.PROVIDER;

public interface PostgresRuntimes {


    @RegisterExtension
    BeforeAllCallback CREATE_DATABASES = context -> {
        createDatabase(CONSUMER.getName());
        createDatabase(PROVIDER.getName());
    };

    String[] CONTROL_PLANE_POSTGRESQL_MODULES = new String[]{
            ":system-tests:e2e-transfer-test:control-plane",
            ":extensions:control-plane:transfer:transfer-data-plane",
            ":extensions:data-plane:data-plane-client",
            ":extensions:control-plane:store:sql:control-plane-sql",
            ":extensions:common:sql:sql-pool:sql-pool-apache-commons",
            ":extensions:common:transaction:transaction-local",
            ":extensions:common:api:management-api-configuration",
            ":extensions:policy-monitor:store:sql:policy-monitor-store-sql"
    };

    String[] DATA_PLANE_POSTGRESQL_MODULES = new String[]{
            ":system-tests:e2e-transfer-test:data-plane",
            ":extensions:data-plane:data-plane-public-api",
            ":extensions:data-plane:store:sql:data-plane-store-sql",
            ":extensions:common:sql:sql-pool:sql-pool-apache-commons",
            ":extensions:common:transaction:transaction-local"
    };

    @RegisterExtension
    EdcClassRuntimesExtension RUNTIMES = new EdcClassRuntimesExtension(
            new EdcRuntimeExtension(
                    "consumer-control-plane",
                    CONSUMER.controlPlanePostgresConfiguration(),
                    CONTROL_PLANE_POSTGRESQL_MODULES
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
                    PROVIDER.dataPlanePostgresConfiguration(),
                    DATA_PLANE_POSTGRESQL_MODULES
            ),
            new EdcRuntimeExtension(
                    "provider-control-plane",
                    PROVIDER.controlPlanePostgresConfiguration(),
                    CONTROL_PLANE_POSTGRESQL_MODULES
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
