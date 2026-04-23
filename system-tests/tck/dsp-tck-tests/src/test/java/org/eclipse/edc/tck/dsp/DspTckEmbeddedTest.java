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
import org.eclipse.dataspacetck.dsp.system.DspSystemLauncher;
import org.eclipse.dataspacetck.runtime.TckRuntime;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.tck.TckTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@TckTest
public class DspTckEmbeddedTest {

    private static final URI PROTOCOL_URL = URI.create("http://localhost:" + getFreePort() + "/api/dsp");
    private static final URI WEBHOOK_URL = URI.create("http://localhost:" + getFreePort() + "/tck");

    @RegisterExtension
    static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
            .name("runtime")
            .modules(":system-tests:tck:dsp-tck-connector-under-test")
            .endpoints(Endpoints.Builder.newInstance()
                    .endpoint("protocol", () -> PROTOCOL_URL)
                    .endpoint("tck", () -> WEBHOOK_URL)
                    .endpoint("management", () -> URI.create("http://localhost:%d/api/management".formatted(getFreePort())))
                    .build())
            .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                    "edc.participant.id", "participantContextId",
                    "web.api.auth.key", "password",
                    "edc.component.id", "DSP-compatibility-test"
            )))
            .build();

    @Timeout(300)
    @Test
    void assertDspCompatibility() throws IOException {
        var result = TckRuntime.Builder.newInstance()
                .properties(loadProperties())
                .launcher(DspSystemLauncher.class)
                .addPackage("org.eclipse.dataspacetck.dsp.verification")
                .monitor(new ConsoleMonitor(true, true))
                .build()
                .execute();

        assertThat(result.getTestsFoundCount()).isGreaterThan(0);

        var failures = result.getFailures().stream()
                .map(this::mapFailure)
                .toList();

        var failureReasons = failures.stream()
                .map(TestResult::format)
                .toList();

        assertThat(failureReasons)
                .withFailMessage(() -> failureReasons.size() + " TCK test cases failed:\n" + String.join("\n", failureReasons))
                .isEmpty();
    }

    private String resourceConfig(String resource) {
        return Path.of(TestUtils.getResource(resource)).toString();
    }

    private Map<String, String> loadProperties() throws IOException {
        var properties = new Properties();
        try (var reader = new FileReader(resourceConfig("docker.tck.properties"))) {
            properties.load(reader);
        }

        properties.put("dataspacetck.dsp.jsonld.context.edc.path", resourceConfig("dspace-edc-context-v1.jsonld"));
        properties.put("dataspacetck.dsp.connector.http.url", PROTOCOL_URL + "/2025-1");
        properties.put("dataspacetck.dsp.connector.http.base.url", PROTOCOL_URL);
        properties.put("dataspacetck.dsp.connector.negotiation.initiate.url", WEBHOOK_URL + "/negotiations/requests");
        properties.put("dataspacetck.dsp.connector.transfer.initiate.url", WEBHOOK_URL + "/transfers/requests");
        return properties.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
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

