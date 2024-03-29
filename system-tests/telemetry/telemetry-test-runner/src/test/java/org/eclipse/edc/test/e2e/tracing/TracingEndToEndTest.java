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

import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.Span;
import jakarta.json.Json;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.stop.Stop;

import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * The role of this class is to test the opentelemetry traces. It only works if the opentelemetry java agent is
 * attached. The java agent trace exporter is configured to use the otlp exporter with http/protobuf protocol with the
 * otel.exporter.otlp.protocol jvm argument.
 */
@EndToEndTest
public class TracingEndToEndTest extends BaseTelemetryEndToEndTest {

    // Port of endpoint to export the traces. 4318 is the default port when protocol is http/protobuf.
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#otlp-exporter-span-metric-and-log-exporters
    private static final int EXPORTER_ENDPOINT_PORT = 4318;
    // Server running to collect traces. The opentelemetry java agent is configured to export traces with the
    // http/protobuf protocol.
    private static ClientAndServer traceCollectorServer;

    @BeforeAll
    public static void setUp() {
        traceCollectorServer = ClientAndServer.startClientAndServer(EXPORTER_ENDPOINT_PORT);
    }

    @AfterAll
    public static void tearDown() {
        Stop.stopQuietly(traceCollectorServer);
    }

    @Test
    void transferFile_testTraces() {
        traceCollectorServer.when(HttpRequest.request()).respond(HttpResponse.response().withStatusCode(200));

        var requestJson = Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                .add(TYPE, "ContractRequest")
                .add("counterPartyAddress", "test-address")
                .add("protocol", "test-protocol")
                .add("providerId", "test-provider-id")
                .add("policy", Json.createObjectBuilder()
                        .add(CONTEXT, "http://www.w3.org/ns/odrl.jsonld")
                        .add(TYPE, "Offer")
                        .add(ID, "offer-id")
                        .add("assigner", "providerId")
                        .add("target", "test-asset")
                        .build())
                .build();

        given()
                .port(MANAGEMENT_PORT)
                .when()
                .contentType(JSON)
                .body(requestJson)
                .post("/management/v2/contractnegotiations")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString(ID);

        await().atMost(30, SECONDS).untilAsserted(() -> {
                    var requests = traceCollectorServer.retrieveRecordedRequests(HttpRequest.request());
                    var spans = extractSpansFromRequests(requests);

                    assertThat(spans.stream())
                            .map(Span::getName)
                            .filteredOn(it -> !it.startsWith("HTTP"))
                            .contains("ConsumerContractNegotiationManagerImpl.initiate");
                }
        );
    }

    /**
     * Extract spans from http requests received by a trace collector.
     *
     * @param requests Request received by an http server trace collector
     * @return spans extracted from the request body
     */
    private List<Span> extractSpansFromRequests(HttpRequest[] requests) {
        return Arrays.stream(requests).map(HttpRequest::getBody)
                .map(body -> {
                    try {
                        return ExportTraceServiceRequest.parseFrom(body.getRawBytes());
                    } catch (InvalidProtocolBufferException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .flatMap(r -> r.getResourceSpansList().stream())
                .flatMap(r -> r.getScopeSpansList().stream())
                .flatMap(r -> r.getSpansList().stream())
                .collect(Collectors.toList());
    }

}
