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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.proto.trace.v1.Span;
import org.eclipse.dataspaceconnector.common.annotations.OpenTelemetryIntegrationTest;
import org.eclipse.dataspaceconnector.system.tests.utils.FileTransferSimulationUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.system.tests.utils.GatlingUtils.runGatling;

/**
 * The role of this class is to test the opentelemetry traces.
 * It only works if the opentelemetry java agent is attached.
 * The default trace exporter configured in the java agent is the OTLP exporter based on gRPC protocol.
 * That's why an OtlpGrpcServer is used to collect the traces.
 */
@OpenTelemetryIntegrationTest
public class TracingIntegrationTest extends FileTransferEdcRuntime {

    static OtlpGrpcServer grpcServer = new OtlpGrpcServer();

    String[] contractNegotiationSpanNames = new String[] {
            "ConsumerContractNegotiationManagerImpl.initiate",
            "ProviderContractNegotiationManagerImpl.requested",
            "ConsumerContractNegotiationManagerImpl.confirmed"
    };

    String[] transferProcessSpanNames = new String[] {
            "TransferProcessManagerImpl.initiateConsumerRequest",
            "TransferProcessManagerImpl.processInitial",
            "TransferProcessManagerImpl.processProvisioned",
            "TransferProcessManagerImpl.initiateProviderRequest"
    };

    @BeforeAll
    static void startGrpcServer() {
        grpcServer.start();
    }

    @AfterAll
    static void stopGrpcServer() {
        grpcServer.stop().join();
    }

    @BeforeEach
    void resetGrpcServer() {
        grpcServer.reset();
        GlobalOpenTelemetry.resetForTest();
    }

    @Test
    void transferFile_testTraces() throws Exception {
        // Arrange
        // Create a file with test data on provider file system.
        String fileContent = "FileTransfer-tracing-test-" + UUID.randomUUID();
        Files.write(Path.of(PROVIDER_ASSET_PATH), fileContent.getBytes(StandardCharsets.UTF_8));

        // Act
        runGatling(FileTransferLocalSimulation.class, FileTransferSimulationUtils.DESCRIPTION);

        // Assert
        await().atMost(30, SECONDS).untilAsserted(() -> {
                    // Get exported spans.
                    List<Span> spans = grpcServer
                            .traceRequests
                            .stream()
                            .flatMap(r -> r.getResourceSpansList().stream())
                            .flatMap(r -> r.getInstrumentationLibrarySpansList().stream())
                            .flatMap(r -> r.getSpansList().stream())
                            .collect(Collectors.toList());

                    // Assert that expected spans are present.
                    List<Span> contractNegotiationSpans = getSpans(spans, Arrays.stream(contractNegotiationSpanNames));
                    List<Span> transferProcessSpans = getSpans(spans, Arrays.stream(transferProcessSpanNames));

                    // Assert that spans are part of the right trace.
                    assertSpansHaveSameTrace(contractNegotiationSpans);
                    assertSpansHaveSameTrace(transferProcessSpans);
                }
        );
    }

    private List<Span> getSpans(List<Span> spans, Stream<String> spanNames) {
        return spanNames.map(spanName -> getSpanByName(spans, spanName)).collect(Collectors.toList());
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
