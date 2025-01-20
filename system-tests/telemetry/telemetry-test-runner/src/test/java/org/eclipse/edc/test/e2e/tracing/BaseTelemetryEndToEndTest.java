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

package org.eclipse.edc.test.e2e.tracing;

import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.function.Supplier;

import static java.util.Map.entry;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public abstract class BaseTelemetryEndToEndTest {

    protected static final int MANAGEMENT_PORT = getFreePort();
    protected static final int PROTOCOL_PORT = getFreePort();
    protected static final int DEFAULT_PORT = getFreePort();

    private static final Supplier<Config> CONFIGURATION_PROVIDER = () -> ConfigFactory.fromMap(Map.ofEntries(
            entry("web.http.path", "/"),
            entry("web.http.port", String.valueOf(DEFAULT_PORT)),
            entry("web.http.control.port", String.valueOf(getFreePort())),
            entry("web.http.protocol.path", "/protocol"),
            entry("web.http.protocol.port", String.valueOf(PROTOCOL_PORT)),
            entry("edc.dsp.callback.address", "http://localhost:" + PROTOCOL_PORT + "/protocol"),
            entry("web.http.management.path", "/management"),
            entry("web.http.management.port", String.valueOf(MANAGEMENT_PORT))
    ));

    @RegisterExtension
    static RuntimeExtension controlPlane = new RuntimePerClassExtension(new EmbeddedRuntime(
            "control-plane",
            ":system-tests:telemetry:telemetry-test-runtime"
    ).configurationProvider(CONFIGURATION_PROVIDER));
}
