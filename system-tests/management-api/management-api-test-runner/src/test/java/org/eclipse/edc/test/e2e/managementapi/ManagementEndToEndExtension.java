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
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import java.util.Map;
import java.util.function.Supplier;

import static java.util.Map.entry;
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
            super(context(ConfigFactory::empty));
        }

        protected InMemory(Supplier<Config> configProvider) {
            super(context(configProvider));
        }

        public static InMemory withConfig(Supplier<Config> configProvider) {
            return new InMemory(configProvider);
        }

        private static ManagementEndToEndTestContext context(Supplier<Config> config) {
            var managementPort = getFreePort();
            var protocolPort = getFreePort();

            var runtime = new EmbeddedRuntime("control-plane",
                    ":system-tests:management-api:management-api-test-runtime")
                    .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                            "web.http.protocol.path", "/protocol",
                            "web.http.protocol.port", String.valueOf(protocolPort),
                            "edc.dsp.callback.address", "http://localhost:" + protocolPort + "/protocol",
                            "web.http.management.path", "/management",
                            "web.http.management.port", String.valueOf(managementPort)
                    )))
                    .configurationProvider(config);

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

            var runtime = new EmbeddedRuntime("control-plane",
                    ":system-tests:management-api:management-api-test-runtime",
                    ":dist:bom:controlplane-feature-sql-bom")
                    .configurationProvider(() -> ConfigFactory.fromMap(Map.ofEntries(
                            entry("web.http.path", "/"),
                            entry("web.http.port", String.valueOf(getFreePort())),
                            entry("web.http.protocol.path", "/protocol"),
                            entry("web.http.protocol.port", String.valueOf(protocolPort)),
                            entry("web.http.control.port", String.valueOf(getFreePort())),
                            entry("edc.dsp.callback.address", "http://localhost:" + protocolPort + "/protocol"),
                            entry("web.http.management.path", "/management"),
                            entry("web.http.management.port", String.valueOf(managementPort)),
                            entry("edc.datasource.default.url", PostgresqlEndToEndInstance.JDBC_URL_PREFIX + "runtime"),
                            entry("edc.datasource.default.user", PostgresqlEndToEndInstance.USER),
                            entry("edc.datasource.default.password", PostgresqlEndToEndInstance.PASSWORD),
                            entry("edc.sql.schema.autocreate", "true")
                    )));

            return new ManagementEndToEndTestContext(runtime, managementPort, protocolPort);
        }

        @Override
        public void beforeAll(ExtensionContext extensionContext) {
            createDatabase("runtime");
            super.beforeAll(extensionContext);
        }
    }
}
