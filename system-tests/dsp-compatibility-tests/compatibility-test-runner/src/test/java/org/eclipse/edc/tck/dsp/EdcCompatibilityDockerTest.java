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
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.eclipse.edc.tck.dsp.CompatibilityTests.ALLOWED_FAILURES;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@NightlyTest
@Testcontainers
public class EdcCompatibilityDockerTest {

    private static final GenericContainer<?> TCK_CONTAINER = new TckContainer<>("eclipsedataspacetck/dsp-tck-runtime:latest");
    @RegisterExtension
    protected static RuntimeExtension runtime = new RuntimePerClassExtension(new EmbeddedRuntime("CUT",
            ":system-tests:dsp-compatibility-tests:connector-under-test"
    ).configurationProvider(EdcCompatibilityDockerTest::runtimeConfiguration));


    private static Config runtimeConfiguration() {
        return ConfigFactory.fromMap(new HashMap<>() {
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
                put("edc.transfer.proxy.token.signer.privatekey.alias", "private-key");
                put("edc.transfer.proxy.token.verifier.publickey.alias", "public-key");
            }
        });
    }

    private static String resourceConfig(String resource) {
        return Path.of(TestUtils.getResource(resource)).toString();
    }

    @Timeout(300)
    @Test
    void assertDspCompatibility() {


        // pipe the docker container's log to this console at the INFO level
        var monitor = new ConsoleMonitor(">>> TCK Runtime (Docker)", ConsoleMonitor.Level.INFO, true);
        var reporter = new TckTestReporter();

        TCK_CONTAINER.addFileSystemBind(resourceConfig("docker.tck.properties"), "/etc/tck/config.properties", BindMode.READ_ONLY, SelinuxContext.SINGLE);
        TCK_CONTAINER.addFileSystemBind(resourceConfig("dspace-edc-context-v1.jsonld"), "/etc/tck/dspace-edc-context-v1.jsonld", BindMode.READ_ONLY, SelinuxContext.SINGLE);
        TCK_CONTAINER.withExtraHost("host.docker.internal", "host-gateway");
        TCK_CONTAINER.withLogConsumer(outputFrame -> monitor.info(outputFrame.getUtf8String()));
        TCK_CONTAINER.withLogConsumer(reporter);
        TCK_CONTAINER.waitingFor(new LogMessageWaitStrategy().withRegEx(".*Test run complete.*").withStartupTimeout(Duration.ofSeconds(300)));
        TCK_CONTAINER.start();

        var failures = reporter.failures();

        assertThat(failures).containsAll(ALLOWED_FAILURES);

        failures.removeAll(ALLOWED_FAILURES);

        if (!failures.isEmpty()) {
            fail(failures.size() + " TCK test cases failed:\n" + String.join("\n", failures));
        }
    }


}

