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
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.Config;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@TckTest
public class DpsTckTest {

    private static final int TCK_PORT = getFreePort();

    private static final URI TCK_URL = URI.create("http://localhost:" + TCK_PORT + "/tck");
    private static final LazySupplier<URI> PROTOCOL_URL = new LazySupplier<>(() -> URI.create("http://localhost:%d/protocol".formatted(getFreePort())));

    @RegisterExtension
    static RuntimeExtension runtime = new RuntimePerClassExtension(new EmbeddedRuntime("CUT",
            ":system-tests:tck:dps-tck-connector-under-test"
    ).configurationProvider(DpsTckTest::runtimeConfiguration));

    private static Config runtimeConfiguration() {
        return ConfigFactory.fromMap(new HashMap<>() {
            {
                put("edc.participant.id", "participantContextId");
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api");
                put("web.http.management.port", String.valueOf(getFreePort()));
                put("web.http.management.path", "/api/management");
                put("web.http.protocol.port", String.valueOf(PROTOCOL_URL.get().getPort()));
                put("web.http.protocol.path", PROTOCOL_URL.get().getPath());
                put("web.http.control.port", String.valueOf(getFreePort()));
                put("web.http.control.path", "/api/control");
                put("web.http.signaling.port", String.valueOf(getFreePort()));
                put("web.http.signaling.path", "/api/signaling");
                put("web.http.tck.port", String.valueOf(TCK_PORT));
                put("web.http.tck.path", "/tck");
                put("web.api.auth.key", "password");
                put("edc.dsp.callback.address", "http://localhost:" + getFreePort() + "/api/dsp");
                put("edc.management.context.enabled", "true");
                put("edc.component.id", "DPS-tck-test");
                put("edc.transfer.proxy.token.signer.privatekey.alias", "private-key");
                put("edc.transfer.proxy.token.verifier.publickey.alias", "public-key");
            }
        });
    }

    @Timeout(300)
    @Test
    void assertDpsCompliance() throws IOException {

        var properties = loadPropertiesFrom("dps.tck.properties");

        properties.put("dataspacetck.debug", "true");
        properties.put("dataspacetck.dps.controlplane.webhook.url", TCK_URL.toString());
        properties.put("dataspacetck.dps.controlplane.protocol.url", PROTOCOL_URL.get() + "/2025-1");

        var result = TckRuntime.Builder.newInstance()
                .properties(properties)
                .launcher(DpsSystemLauncher.class)
                .addPackage("org.eclipse.dataspacetck.dps.verification.controlplane")
                .displayNameMatching(it -> it.startsWith("CP_C:02-01"))
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
