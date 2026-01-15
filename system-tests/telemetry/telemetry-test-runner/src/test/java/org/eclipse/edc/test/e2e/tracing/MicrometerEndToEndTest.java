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

package org.eclipse.edc.test.e2e.tracing;

import jakarta.json.Json;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@EndToEndTest
public class MicrometerEndToEndTest extends BaseTelemetryEndToEndTest {

    private static final int METRICS_PORT = 9464;

    @Test
    void testMicrometerMetrics() {
        // will request the catalog to itself, just to trigger okhttp metrics
        var requestBody = Json.createObjectBuilder()
                .add(TYPE, CATALOG_REQUEST_TYPE)
                .add(EDC_NAMESPACE + "counterPartyAddress", "http://localhost:" + PROTOCOL_PORT + "/protocol/2025-1")
                .add(EDC_NAMESPACE + "protocol", "dataspace-protocol-http:2025-1")
                .build();

        given()
                .port(MANAGEMENT_PORT)
                .contentType(JSON)
                .body(requestBody)
                .post("/management/v3/catalog/request")
                .then()
                .statusCode(200);

        var metricsResponseBody = given()
                .port(METRICS_PORT)
                .get("/metrics")
                .then()
                .statusCode(200)
                .extract().body().asString();

        var metrics = metricsResponseBody.split("\n");

        assertThat(metrics)
                .anyMatch(it -> it.startsWith("executor_")) // ExecutorMetrics added by MicrometerExtension
                .anyMatch(it -> it.startsWith("jvm_memory_")) // JvmMemoryMetrics added by MicrometerExtension
                .anyMatch(it -> it.startsWith("jvm_gc")) // JvmGcMetrics added by MicrometerExtension
                .anyMatch(it -> it.startsWith("system_cpu_")) // ProcessorMetrics added by MicrometerExtension
                .anyMatch(it -> it.startsWith("jvm_threads_")) // JvmThreadMetrics added by MicrometerExtension
                .anyMatch(it -> it.startsWith("jetty_")) // Added by JettyMicrometerExtension
                .anyMatch(it -> it.startsWith("http_client_")); // OkHttp metrics
    }
}
