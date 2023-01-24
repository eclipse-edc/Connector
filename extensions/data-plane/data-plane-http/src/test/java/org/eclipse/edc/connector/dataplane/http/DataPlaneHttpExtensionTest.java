/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.http;

import org.eclipse.edc.connector.dataplane.http.pipeline.HttpParamsDecoratorRegistry;
import org.eclipse.edc.connector.dataplane.http.pipeline.HttpParamsDecoratorRegistryImpl;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.stop.Stop.stopQuietly;

@ExtendWith(EdcExtension.class)
public class DataPlaneHttpExtensionTest {

    private static ClientAndServer sourceServer;
    private static ClientAndServer destinationServer;
    private static final int SOURCE_PORT = getFreePort();
    private static final int DESTINATION_PORT = getFreePort();

    @BeforeAll
    public static void setUp() {
        sourceServer = startClientAndServer(SOURCE_PORT);
        destinationServer = startClientAndServer(DESTINATION_PORT);
    }

    @AfterAll
    public static void tearDown() {
        stopQuietly(sourceServer);
        stopQuietly(destinationServer);
    }

    @Test
    void transferSourceToDestination(PipelineService pipeline) {
        var source = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://localhost:" + SOURCE_PORT)
                .build();
        var destination = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://localhost:" + DESTINATION_PORT)
                .build();
        sourceServer.when(request()).respond(HttpResponse.response().withStatusCode(200));
        destinationServer.when(request()).respond(HttpResponse.response().withStatusCode(200));

        var request = DataFlowRequest.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .traceContext(emptyMap())
                .build();
        var future = pipeline.transfer(request);

        assertThat(future).succeedsWithin(10, SECONDS)
                .matches(StatusResult::succeeded);
        sourceServer.verify(request().withMethod("GET"));
        destinationServer.verify(request().withMethod("POST"));
    }

    @Test
    void transferSourceToDestinationAddHeaders(PipelineService pipeline, HttpParamsDecoratorRegistry decoratorRegistry) {
        decoratorRegistry.registerSourceDecorator((request, builder) -> builder.header("customSourceHeader", "customValue"));
        decoratorRegistry.registerSinkDecorator((request, builder) -> builder.header("customSinkHeader", "customValue"));
        var source = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://localhost:" + SOURCE_PORT)
                .build();
        var destination = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://localhost:" + DESTINATION_PORT)
                .build();
        sourceServer.when(request()).respond(HttpResponse.response().withStatusCode(200));
        destinationServer.when(request()).respond(HttpResponse.response().withStatusCode(200));

        var request = DataFlowRequest.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .traceContext(emptyMap())
                .build();
        var future = pipeline.transfer(request);

        assertThat(future).succeedsWithin(10, SECONDS)
                .matches(StatusResult::succeeded);
        sourceServer.verify(request().withMethod("GET").withHeader("customSourceHeader", "customValue"));
        destinationServer.verify(request().withMethod("POST").withHeader("customSinkHeader", "customValue"));
    }
}
