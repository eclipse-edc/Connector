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
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.HashMap;

import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.createDatabase;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public abstract class ManagementEndToEndExtension implements ParameterResolver, BeforeAllCallback, AfterAllCallback {

    protected final EdcRuntimeExtension runtime;
    private final ManagementEndToEndTestContext context;

    protected ManagementEndToEndExtension(ManagementEndToEndTestContext context) {
        this.context = context;
        this.runtime = context.runtime();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return true;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(EdcRuntimeExtension.class)) {
            return runtime;
        }
        if (type.equals(ManagementEndToEndTestContext.class)) {
            return context;
        }
        return runtime.getService((Class<?>) type);
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        runtime.beforeTestExecution(extensionContext);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        runtime.afterTestExecution(extensionContext);
    }

    static class InMemory extends ManagementEndToEndExtension {

        private static ManagementEndToEndTestContext context() {
            var managementPort = getFreePort();
            var protocolPort = getFreePort();

            var runtime = new EdcRuntimeExtension(
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
                        }
                    },
                    ":system-tests:management-api:management-api-test-runtime"
            );

            return new ManagementEndToEndTestContext(runtime, managementPort, protocolPort);
        }

        protected InMemory() {
            super(context());
        }

    }

    static class Postgres extends ManagementEndToEndExtension {

        private static ManagementEndToEndTestContext context() {
            var managementPort = getFreePort();
            var protocolPort = getFreePort();

            var runtime = new EdcRuntimeExtension(
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
                        }
                    },
                    ":system-tests:management-api:management-api-test-runtime",
                    ":extensions:control-plane:store:sql:control-plane-sql",
                    ":extensions:common:sql:sql-pool:sql-pool-apache-commons",
                    ":extensions:common:transaction:transaction-local"
            );

            return new ManagementEndToEndTestContext(runtime, managementPort, protocolPort);
        }

        protected Postgres() {
            super(context());
        }

        @Override
        public void beforeAll(ExtensionContext extensionContext) throws Exception {
            createDatabase("runtime");
            super.beforeAll(extensionContext);
        }
    }
}
