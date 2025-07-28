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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.spi.port.TransferProcessApiClient;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.matching.UrlPattern.ANY;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ComponentTest
public class DataPlaneHttpExtensionTest {

    @RegisterExtension
    private static final RuntimeExtension RUNTIME = new RuntimePerClassExtension()
            .registerServiceMock(TransferProcessApiClient.class, mock());
    @RegisterExtension
    static WireMockExtension sourceServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static WireMockExtension destinationServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();


    @Test
    void transferSourceToDestination(PipelineService pipelineService) {
        var source = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://localhost:" + sourceServer.getPort())
                .build();
        var destination = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://localhost:" + destinationServer.getPort())
                .build();

        sourceServer.stubFor(get(ANY).willReturn(ok()));
        destinationServer.stubFor(post(ANY).willReturn(ok()));

        var request = DataFlowStartMessage.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .traceContext(emptyMap())
                .build();

        var future = pipelineService.transfer(request);

        assertThat(future).succeedsWithin(10, SECONDS)
                .matches(StreamResult::succeeded);
        sourceServer.verify(getRequestedFor(anyUrl()));
        destinationServer.verify(postRequestedFor(anyUrl()));
    }

    @Test
    void transferSourceToDestinationAddHeaders(PipelineService pipelineService, HttpRequestParamsProvider paramsProvider) {
        paramsProvider.registerSourceDecorator((request, address, builder) -> builder.header("customSourceHeader", "customValue"));
        paramsProvider.registerSinkDecorator((request, address, builder) -> builder.header("customSinkHeader", "customValue"));
        var source = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://localhost:" + sourceServer.getPort())
                .build();
        var destination = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://localhost:" + destinationServer.getPort())
                .build();
        sourceServer.stubFor(get(ANY).willReturn(ok()));
        destinationServer.stubFor(post(ANY).willReturn(ok()));

        var request = DataFlowStartMessage.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .traceContext(emptyMap())
                .build();

        var future = pipelineService.transfer(request);

        assertThat(future).succeedsWithin(10, SECONDS)
                .matches(StreamResult::succeeded);

        sourceServer.verify(getRequestedFor(anyUrl()).withHeader("customSourceHeader", equalTo("customValue")));
        destinationServer.verify(postRequestedFor(anyUrl()).withHeader("customSinkHeader", equalTo("customValue")));

    }
}
