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

package org.eclipse.edc.tck.dsp;

import org.eclipse.edc.junit.annotations.NightlyTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.util.HashMap;

import static org.eclipse.edc.util.io.Ports.getFreePort;

@Disabled(value = "Need to disable this test until the connector is 100% compliant with DSP")
@NightlyTest
@Testcontainers
public class EdcCompatibilityTest {

    private static final GenericContainer<?> TCK_CONTAINER = new TckContainer<>("eclipsedataspacetck/dsp-tck-runtime:latest");

    private static String resource(String s) {
        return Path.of(TestUtils.getResource("docker.tck.properties")).toString();
    }

    @RegisterExtension
    protected static RuntimeExtension runtime =
            new RuntimePerClassExtension(new EmbeddedRuntime("CUnT",
                    new HashMap<>() {
                        {
                            put("edc.participant.id", "CONNECTOR_UNDER_TEST");
                            put("web.http.port", "8080");
                            put("web.http.path", "/api");
                            put("web.http.version.port", String.valueOf(getFreePort()));
                            put("web.http.version.path", "/api/version");
                            put("web.http.control.port", String.valueOf(getFreePort()));
                            put("web.http.control.path", "/api/control");
                            put("web.http.management.port", "8081");
                            put("web.http.management.path", "/api/management");
                            put("web.http.protocol.port", "8282"); // this must match the configured connector url in resources/docker.tck.properties
                            put("web.http.protocol.path", "/api/dsp"); // this must match the configured connector url in resources/docker.tck.properties
                            put("web.api.auth.key", "password");
                            put("edc.dsp.callback.address", "http://host.docker.internal:8282/api/dsp"); // host.docker.internal is required by the container to communicate with the host
                            put("edc.management.context.enabled", "true");
                            put("edc.hostname", "host.docker.internal");
                            put("edc.component.id", "DSP-compatibility-test");
                        }
                    },
                    ":system-tests:dsp-compatibility-tests:connector-under-test"
            ));

    @Timeout(60)
    @Test
    void assertDspCompatibility() {
        // pipe the docker container's log to this console at the INFO level
        var monitor = new ConsoleMonitor(">>> TCK Runtime (Docker)", ConsoleMonitor.Level.INFO, true);
        TCK_CONTAINER.addFileSystemBind(resource("docker.tck.properties"), "/etc/tck/config.properties", BindMode.READ_ONLY, SelinuxContext.SINGLE);
        TCK_CONTAINER.withLogConsumer(outputFrame -> monitor.info(outputFrame.getUtf8String()));
        TCK_CONTAINER.waitingFor(new LogMessageWaitStrategy().withRegEx(".*Test run complete.*"));
        TCK_CONTAINER.start();

        // todo: obtain test report from the container
    }
}

