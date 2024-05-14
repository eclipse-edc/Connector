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

package org.eclipse.edc.test.e2e.managementapi;

import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public interface Runtimes {

    static EdcRuntimeExtension inMemoryRuntime() {
        return new EdcRuntimeExtension(
                "control-plane",
                inMemoryConfiguration(),
                ":system-tests:management-api:management-api-test-runtime"
        );
    }

    static EdcRuntimeExtension postgresRuntime() {
        return new EdcRuntimeExtension(
                "control-plane",
                postgresqlConfiguration(),
                ":system-tests:management-api:management-api-test-runtime",
                ":extensions:control-plane:store:sql:control-plane-sql",
                ":extensions:common:sql:sql-pool:sql-pool-apache-commons",
                ":extensions:common:transaction:transaction-local"
        );
    }

    @NotNull
    static HashMap<String, String> inMemoryConfiguration() {
        return new HashMap<>() {
            {
                put("web.http.path", "/");
                put("web.http.protocol.path", "/protocol");
                put("web.http.protocol.port", String.valueOf(ManagementApiEndToEndTestBase.PROTOCOL_PORT));
                put("edc.dsp.callback.address", "http://localhost:" + ManagementApiEndToEndTestBase.PROTOCOL_PORT + "/protocol");
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.control.port", String.valueOf(getFreePort()));
                put("web.http.management.path", "/management");
                put("web.http.management.port", String.valueOf(ManagementApiEndToEndTestBase.PORT));
            }
        };
    }

    @NotNull
    static HashMap<String, String> postgresqlConfiguration() {
        var config = new HashMap<String, String>() {
            {
                put("edc.datasource.default.url", PostgresqlEndToEndInstance.JDBC_URL_PREFIX + "runtime");
                put("edc.datasource.default.user", PostgresqlEndToEndInstance.USER);
                put("edc.datasource.default.password", PostgresqlEndToEndInstance.PASSWORD);
            }
        };

        config.putAll(inMemoryConfiguration());
        return config;
    }
}
