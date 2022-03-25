/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.metrics.micrometer;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.OpenTelemetryIntegrationTest;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;

@OpenTelemetryIntegrationTest
@ExtendWith(EdcExtension.class)
public class MicrometerExtensionIntegrationTest {
    static final int CONNECTOR_PORT = getFreePort();
    static final String HEALTH_ENDPOINT = String.format("http://localhost:%s/api/check/health", CONNECTOR_PORT);
    static final String METRICS_ENDPOINT = "http://localhost:9464/metrics";
    static final String[] METRIC_PREFIXES = new String[] {
            "executor_", // ExecutorMetrics added by MicrometerExtension
            "jvm_memory_", // JvmMemoryMetrics added by MicrometerExtension
            "jvm_gc", // JvmGcMetrics added by MicrometerExtension
            "system_cpu_", // ProcessorMetrics added by MicrometerExtension
            "jvm_threads_", // JvmThreadMetrics added by MicrometerExtension
            "jetty_", // Added by JettyMicrometerExtension
            "jersey_", // Added by JerseyMicrometerExtension
            "http_client_"}; // OkHttp metrics

    @BeforeAll
    static void checkForAgent() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        assertThat(runtimeMxBean.getInputArguments())
                .withFailMessage("OpenTelemetry Agent JAR should be present. See README.md file for details.")
                .anyMatch(arg -> arg.startsWith("-javaagent"));
    }

    @BeforeEach
    void before(EdcExtension extension) {
        extension.setConfiguration(Map.of("web.http.port", String.valueOf(CONNECTOR_PORT)));
    }

    @Test
    void testMicrometerMetrics(OkHttpClient httpClient) throws IOException {
        // Call the health endpoint with the client used by the connector. This is needed to check if OkHttp metrics are present.
        Response healthResponse = httpClient.newCall(new Request.Builder().url(HEALTH_ENDPOINT).build()).execute();
        assertThat(healthResponse.code())
                .withFailMessage("Failed calling health endpoint. The call needs to be successful to have Jetty & Jersey metrics.")
                .isEqualTo(200);

        // Collect the metrics.
        Response response = httpClient.newCall(new Request.Builder().url(METRICS_ENDPOINT).build()).execute();
        String[] metrics = response.body().string().split("\n");

        for (String metricPrefix : METRIC_PREFIXES) {
            assertThat(metrics)
                    .withFailMessage(String.format("There is no metric starting by %s. ", metricPrefix))
                    .anyMatch(s -> s.startsWith(metricPrefix));
        }
    }
}
