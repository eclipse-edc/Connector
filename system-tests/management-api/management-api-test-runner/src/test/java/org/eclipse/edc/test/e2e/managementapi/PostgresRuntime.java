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

import org.eclipse.edc.junit.extensions.EdcClassRuntimesExtension;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;

import static org.eclipse.edc.test.e2e.managementapi.InMemoryRuntime.inMemoryConfiguration;

public interface PostgresRuntime {

    EdcRuntimeExtension RUNTIME = new EdcRuntimeExtension(
            "control-plane",
            postgresqlConfiguration(),
            ":system-tests:management-api:management-api-test-runtime",
            ":extensions:control-plane:store:sql:control-plane-sql",
            ":extensions:common:sql:sql-pool:sql-pool-apache-commons",
            ":extensions:common:transaction:transaction-local"
    );

    @RegisterExtension
    EdcClassRuntimesExtension RUNTIMES = new EdcClassRuntimesExtension(RUNTIME);

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
