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

import org.eclipse.dataspacetck.core.system.ConsoleMonitor;
import org.eclipse.dataspacetck.runtime.TckRuntime;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspacetck.core.api.system.SystemsConstants.TCK_LAUNCHER;
import static org.eclipse.edc.tck.dsp.CompatibilityTests.ALLOWED_FAILURES;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@EndToEndTest
public class EdcCompatibilityEmbeddedTest {

    private static final URI PROTOCOL_URL = URI.create("http://localhost:" + getFreePort() + "/api/dsp");
    private static final URI WEBHOOK_URL = URI.create("http://localhost:" + getFreePort() + "/tck");
    private static final String DEFAULT_LAUNCHER = "org.eclipse.dataspacetck.dsp.system.DspSystemLauncher";
    private static final String TEST_PACKAGE = "org.eclipse.dataspacetck.dsp.verification";

    @RegisterExtension
    protected static RuntimeExtension runtime = new RuntimePerClassExtension(new EmbeddedRuntime("CUT",
            ":system-tests:dsp-compatibility-tests:connector-under-test"
    ).configurationProvider(EdcCompatibilityEmbeddedTest::runtimeConfiguration));


    private static Config runtimeConfiguration() {
        return ConfigFactory.fromMap(new HashMap<>() {
            {
                put("edc.participant.id", "participantContextId");
                put("web.http.port", "8080");
                put("web.http.path", "/api");
                put("web.http.control.port", String.valueOf(getFreePort()));
                put("web.http.control.path", "/api/control");
                put("web.http.management.port", String.valueOf(getFreePort()));
                put("web.http.management.path", "/api/management");
                put("web.http.protocol.port", String.valueOf(PROTOCOL_URL.getPort())); // this must match the configured connector url in resources/docker.tck.properties
                put("web.http.protocol.path", "/api/dsp"); // this must match the configured connector url in resources/docker.tck.properties
                put("web.http.tck.port", String.valueOf(WEBHOOK_URL.getPort())); // this must match the configured connector url in resources/docker.tck.properties
                put("web.http.tck.path", "/tck"); //
                put("web.api.auth.key", "password");
                put("edc.dsp.callback.address", PROTOCOL_URL.toString()); // host.docker.internal is required by the container to communicate with the host
                put("edc.management.context.enabled", "true");
                put("edc.component.id", "DSP-compatibility-test");
                put("edc.transfer.proxy.token.signer.privatekey.alias", "private-key");
                put("edc.transfer.proxy.token.verifier.publickey.alias", "public-key");
            }
        });
    }

    private static String resourceConfig(String resource) {
        return Path.of(TestUtils.getResource(resource)).toString();
    }

    private static Map<String, String> loadProperties() throws IOException {
        var properties = new Properties();
        try (var reader = new FileReader(resourceConfig("docker.tck.properties"))) {
            properties.load(reader);
        }

        if (!properties.containsKey(TCK_LAUNCHER)) {
            properties.put(TCK_LAUNCHER, DEFAULT_LAUNCHER);
        }
        properties.put("dataspacetck.dsp.jsonld.context.edc.path", resourceConfig("dspace-edc-context-v1.jsonld"));
        properties.put("dataspacetck.dsp.connector.http.url", PROTOCOL_URL + "/2025-1");
        properties.put("dataspacetck.dsp.connector.http.base.url", PROTOCOL_URL);
        properties.put("dataspacetck.dsp.connector.negotiation.initiate.url", WEBHOOK_URL + "/negotiations/requests");
        properties.put("dataspacetck.dsp.connector.transfer.initiate.url", WEBHOOK_URL + "/transfers/requests");
        return properties.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    }

    @Timeout(300)
    @Test
    void assertDspCompatibility() throws IOException {
        var monitor = new ConsoleMonitor(true, true);

        var result = TckRuntime.Builder.newInstance()
                .properties(loadProperties())
                .addPackage(TEST_PACKAGE)
                .monitor(monitor)
                .build()
                .execute();

        var failures = result.getFailures().stream()
                .map(this::mapFailure)
                .toList();

        var failureIds = failures.stream()
                .map(TestResult::testId)
                .collect(Collectors.toSet());

        assertThat(failureIds).containsAll(ALLOWED_FAILURES);

        var failureReasons = failures.stream()
                .filter(f -> !ALLOWED_FAILURES.contains(f.testId))
                .map(TestResult::format)
                .toList();

        assertThat(failureReasons)
                .withFailMessage(() -> failureReasons.size() + " TCK test cases failed:\n" + String.join("\n", failureReasons))
                .isEmpty();

    }

    private TestResult mapFailure(TestExecutionSummary.Failure failure) {
        var displayName = failure.getTestIdentifier().getDisplayName().split(":");
        return new TestResult(format("%s:%s", displayName[0], displayName[1]), failure);
    }

    private record TestResult(String testId, TestExecutionSummary.Failure failure) {
        public String format() {
            return "- " + failure.getTestIdentifier().getDisplayName() + " (" + failure.getException() + ")";
        }
    }
}

