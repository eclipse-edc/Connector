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
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import java.util.HashMap;
import java.util.function.Supplier;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public abstract class ManagementEndToEndExtension extends RuntimePerClassExtension {

    private final ManagementEndToEndTestContext context;

    protected ManagementEndToEndExtension(ManagementEndToEndTestContext context) {
        super(context.runtime());
        this.context = context;
    }

    static Config runtimeConfig() {
        var managementPort = getFreePort();
        var protocolPort = getFreePort();

        var settings = new HashMap<String, String>() {
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
        };

        return ConfigFactory.fromMap(settings);
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

    public ManagementEndToEndExtension withConfig(Supplier<Config> configSupplier) {
        context.runtime().configurationProvider(configSupplier);
        return this;
    }

    public static class InMemory extends ManagementEndToEndExtension {

        protected InMemory() {
            super(new ManagementEndToEndTestContext(runtime()));
        }

        private static @NotNull EmbeddedRuntime runtime() {
            return new EmbeddedRuntime("control-plane", ":system-tests:management-api:management-api-test-runtime")
                    .configurationProvider(ManagementEndToEndExtension::runtimeConfig);
        }
    }

    public static class Postgres extends ManagementEndToEndExtension {

        public Postgres(PostgresqlEndToEndExtension postgres) {
            super(new ManagementEndToEndTestContext(runtime().configurationProvider(postgres::config)));
        }

        private static EmbeddedRuntime runtime() {
            return new EmbeddedRuntime("control-plane",
                    ":system-tests:management-api:management-api-test-runtime",
                    ":dist:bom:controlplane-feature-sql-bom")
                    .configurationProvider(ManagementEndToEndExtension::runtimeConfig);
        }

    }
}
