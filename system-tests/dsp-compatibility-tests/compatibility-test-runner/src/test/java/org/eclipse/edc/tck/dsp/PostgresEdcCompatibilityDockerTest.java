/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dsp;

import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.BeforeAllCallback;
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

@PostgresqlIntegrationTest
@Testcontainers
public class PostgresEdcCompatibilityDockerTest {

    @Order(0)
    @RegisterExtension
    static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();
    private static final String CONNECTOR_UNDER_TEST = "CUT";

    @Order(1)
    @RegisterExtension
    static final BeforeAllCallback CREATE_DATABASES = context -> {
        POSTGRESQL_EXTENSION.createDatabase(CONNECTOR_UNDER_TEST.toLowerCase());
    };

    private static final GenericContainer<?> TCK_CONTAINER = new TckContainer<>("eclipsedataspacetck/dsp-tck-runtime:1.0.0-RC4");
    @RegisterExtension
    protected static RuntimeExtension runtime = new RuntimePerClassExtension(new EmbeddedRuntime(CONNECTOR_UNDER_TEST,
            ":system-tests:dsp-compatibility-tests:connector-under-test",
            ":dist:bom:controlplane-feature-sql-bom"
    ).configurationProvider(PostgresEdcCompatibilityDockerTest::runtimeConfiguration)
            .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(CONNECTOR_UNDER_TEST.toLowerCase())));

    private static Config runtimeConfiguration() {
        return ConfigFactory.fromMap(new HashMap<>() {
            {
                put("edc.participant.id", "participantContextId");
                put("web.http.port", "8080");
                put("web.http.path", "/api");
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

    private String resourceConfig(String resource) {
        return Path.of(TestUtils.getResource(resource)).toString();
    }


}

