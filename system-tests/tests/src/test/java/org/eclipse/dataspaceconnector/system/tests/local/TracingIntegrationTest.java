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
 *       Microsoft Corporation - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.system.tests.local;

import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.Span;
import org.eclipse.dataspaceconnector.common.annotations.OpenTelemetryIntegrationTest;
import org.eclipse.dataspaceconnector.system.tests.utils.FileTransferSimulationUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.system.tests.utils.GatlingUtils.runGatling;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

/**
 * The role of this class is to test the opentelemetry traces.
 * It only works if the opentelemetry java agent is attached.
 * The java agent trace exporter is configured to use the otlp exporter with http/protobuf protocol with the
 * otel.exporter.otlp.protocol jvm argument.
 */
@OpenTelemetryIntegrationTest
public class TracingIntegrationTest extends FileTransferEdcRuntime {

    // Port of endpoint to export the traces. 4318 is the default port when protocol is http/protobuf.
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#otlp-exporter-span-metric-and-log-exporters
    static final int EXPORTER_ENDPOINT_PORT = 4318;
    // Server running to collect traces. The opentelemetry java agent is configured to export traces with the
    // http/protobuf protocol.
    static ClientAndServer traceCollectorServer;

    List<String> contractNegotiationSpanNames = List.of(
            "ConsumerContractNegotiationManagerImpl.initiate", // initial API request
            "ProviderContractNegotiationManagerImpl.requested", // verify context propagation in ProviderContractNegotiationManagerImpl
            "ConsumerContractNegotiationManagerImpl.confirmed" // verify context propagation in ConsumerContractNegotiationManagerImpl
    );

    List<String> transferProcessSpanNames = List.of(
            "TransferProcessManagerImpl.initiateConsumerRequest", // initial API request
            "TransferProcessManagerImpl.processInitial", // verify context propagation in TransferProcessManagerImpl
            "TransferProcessManagerImpl.initiateProviderRequest", // verify context propagation in TransferProcessManagerImpl
            "TransferProcessManagerImpl.processProvisioned", // verify context propagation in TransferProcessManagerImpl
            "EmbeddedDataPlaneTransferClient.transfer", // DPF call
            "PipelineServiceImpl.transfer", // verify context propagation in DataPlaneManagerImpl
            "FileTransferDataSink.transferParts" // verify context propagation in ParallelSink
    );

    @BeforeAll
    public static void setUp() {
        traceCollectorServer = startClientAndServer(EXPORTER_ENDPOINT_PORT);
    }

    @AfterAll
    public static void tearDown() {
        stopQuietly(traceCollectorServer);
    }

    @Test
    void transferFile_testTraces() throws Exception {
        traceCollectorServer.when(request()).respond(response().withStatusCode(200));

        // Arrange
        // Create a file with test data on provider file system.
        String fileContent = "FileTransfer-tracing-test-" + UUID.randomUUID();
        Files.write(Path.of(PROVIDER_ASSET_PATH), fileContent.getBytes(StandardCharsets.UTF_8));

        // Act
        runGatling(FileTransferLocalSimulation.class, FileTransferSimulationUtils.DESCRIPTION);

        // Assert
        await().atMost(30, SECONDS).untilAsserted(() -> {
            // Get exported spans.
            var requests = traceCollectorServer.retrieveRecordedRequests(request());
            var spans = extractSpansFromRequests(requests);
            // Assert that expected spans are present.
            List<Span> contractNegotiationSpans = filterSpansByName(spans, contractNegotiationSpanNames);
            List<Span> transferProcessSpans = filterSpansByName(spans, transferProcessSpanNames);

            // Assert that spans are part of the right trace.
            assertSpansHaveSameTrace(contractNegotiationSpans);
            assertSpansHaveSameTrace(transferProcessSpans);
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
            .flatMap(r -> r.getInstrumentationLibrarySpansList().stream())
            .flatMap(r -> r.getSpansList().stream())
            .collect(Collectors.toList());
    }

    private List<Span> filterSpansByName(List<Span> spans, List<String> spanNames) {
        return spanNames.stream().map(spanName -> getSpanByName(spans, spanName)).collect(Collectors.toList());
    }

    private void assertSpansHaveSameTrace(List<Span> spans) {
        assertThat(spans.stream().map(s -> s.getTraceId().toStringUtf8()).distinct())
                .withFailMessage(() -> "Spans from the same trace should have the same traceId.")
                .singleElement();
    }

    private Span getSpanByName(Collection<Span> spans, String name) {
        var span = spans.stream().filter(s -> name.equals(s.getName())).findFirst();
        assertThat(span)
                .withFailMessage(format("Span %s is missing", name))
                .isPresent();
        return span.orElseThrow();
    }
}
