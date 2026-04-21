/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dps;

import org.eclipse.dataspacetck.dps.system.DpsSystemLauncher;
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
public class DpsTckTest {

    private static final int TCK_CALLBACK_PORT = getFreePort();
    private static final URI TCK_URL = URI.create("http://localhost:" + getFreePort() + "/tck");
    private static final URI PROTOCOL_URL = URI.create("http://localhost:" + getFreePort() + "/protocol");

    @RegisterExtension
    static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
            .name("CUT")
            .modules(":system-tests:tck:dps-tck-connector-under-test")
            .endpoints(Endpoints.Builder.newInstance()
                    .endpoint("default", () -> URI.create("http://localhost:%d/api".formatted(getFreePort())))
                    .endpoint("management", () -> URI.create("http://localhost:%d/api/management".formatted(getFreePort())))
                    .endpoint("protocol", () -> PROTOCOL_URL)
                    .endpoint("signaling", () -> URI.create("http://localhost:%d/api/signaling".formatted(getFreePort())))
                    .endpoint("tck", () -> TCK_URL)
                    .build())
            .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                    "edc.participant.id", "participantContextId",
                    "web.api.auth.key", "password",
                    "edc.management.context.enabled", "true",
                    "edc.component.id", "DPS-tck-test",
                    "edc.tck.dataplane.url", "http://localhost:" + TCK_CALLBACK_PORT
            )))
            .build();

    @Timeout(300)
    @Test
    void assertDpsCompliance() throws IOException {

        var properties = loadPropertiesFrom("dps.tck.properties");

        properties.put("dataspacetck.debug", "true");
        properties.put("dataspacetck.callback.address", "http://localhost:" + TCK_CALLBACK_PORT);
        properties.put("dataspacetck.port", String.valueOf(TCK_CALLBACK_PORT));
        properties.put("dataspacetck.dps.controlplane.webhook.url", TCK_URL.toString());
        properties.put("dataspacetck.dps.controlplane.protocol.url", PROTOCOL_URL + "/2025-1");

        var result = TckRuntime.Builder.newInstance()
                .properties(properties)
                .launcher(DpsSystemLauncher.class)
                .addPackage("org.eclipse.dataspacetck.dps.verification.controlplane")
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
                .withFailMessage(() -> failureReasons.size() + " DPS TCK test cases failed:\n" + String.join("\n", failureReasons))
                .isEmpty();
    }

    private Map<String, String> loadPropertiesFrom(String resource) throws IOException {
        var properties = new Properties();
        try (var reader = new FileReader(resourceConfig(resource))) {
            properties.load(reader);
        }

        return properties.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    }

    private String resourceConfig(String resource) {
        return Path.of(TestUtils.getResource(resource)).toString();
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
