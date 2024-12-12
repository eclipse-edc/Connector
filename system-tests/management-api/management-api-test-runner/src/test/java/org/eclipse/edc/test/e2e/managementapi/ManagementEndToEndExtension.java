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

import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.createDatabase;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public abstract class ManagementEndToEndExtension extends RuntimePerClassExtension {

    private final ManagementEndToEndTestContext context;

    protected ManagementEndToEndExtension(ManagementEndToEndTestContext context) {
        super(context.runtime());
        this.context = context;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(ManagementEndToEndTestContext.class)) {
            return true;
        }
        return super.supportsParameter(parameterContext, extensionContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(ManagementEndToEndTestContext.class)) {
            return context;
        }
        return super.resolveParameter(parameterContext, extensionContext);
    }

    static class InMemory extends ManagementEndToEndExtension {

        protected InMemory() {
            super(context(Map.of()));
        }

        protected InMemory(Map<String, String> config) {
            super(context(config));
        }

        public static InMemory withConfig(Map<String, String> config) {
            return new InMemory(config);
        }

        private static ManagementEndToEndTestContext context(Map<String, String> config) {
            var managementPort = getFreePort();
            var protocolPort = getFreePort();

            var runtime = new EmbeddedRuntime(
                    "control-plane",
                    new HashMap<>() {
                        {
                            put("web.http.protocol.path", "/protocol");
                            put("web.http.protocol.port", String.valueOf(protocolPort));
                            put("edc.dsp.callback.address", "http://localhost:" + protocolPort + "/protocol");
                            put("web.http.management.path", "/management");
                            put("web.http.management.port", String.valueOf(managementPort));
                            putAll(config);
                        }
                    },
                    ":system-tests:management-api:management-api-test-runtime"
            );

            return new ManagementEndToEndTestContext(runtime, managementPort, protocolPort);
        }

    }

    static class Postgres extends ManagementEndToEndExtension {

        protected Postgres() {
            super(context());
        }

        private static ManagementEndToEndTestContext context() {
            var managementPort = getFreePort();
            var protocolPort = getFreePort();

            var runtime = new EmbeddedRuntime(
                    "control-plane",
                    new HashMap<>() {
                        {
                            put("web.http.path", "/");
                            put("web.http.port", String.valueOf(getFreePort()));
                            put("web.http.protocol.path", "/protocol");
                            put("web.http.protocol.port", String.valueOf(protocolPort));
                            put("web.http.control.port", String.valueOf(getFreePort()));
                            put("edc.dsp.callback.address", "http://localhost:" + protocolPort + "/protocol");
                            put("web.http.management.path", "/management");
                            put("web.http.management.port", String.valueOf(managementPort));
                            put("edc.datasource.default.url", PostgresqlEndToEndInstance.JDBC_URL_PREFIX + "runtime");
                            put("edc.datasource.default.user", PostgresqlEndToEndInstance.USER);
                            put("edc.datasource.default.password", PostgresqlEndToEndInstance.PASSWORD);
                            put("edc.sql.schema.autocreate", "true");
                        }
                    },
                    ":system-tests:management-api:management-api-test-runtime",
                    ":extensions:control-plane:store:sql:control-plane-sql",
                    ":extensions:policy-monitor:store:sql:policy-monitor-store-sql",
                    ":extensions:common:store:sql:edr-index-sql",
                    ":extensions:data-plane:store:sql:data-plane-store-sql",
                    ":extensions:common:sql:sql-pool:sql-pool-apache-commons",
                    ":extensions:common:transaction:transaction-local"
            );

            return new ManagementEndToEndTestContext(runtime, managementPort, protocolPort);
        }

        @Override
        public void beforeAll(ExtensionContext extensionContext) {
            createDatabase("runtime");
            super.beforeAll(extensionContext);
        }
    }
}
