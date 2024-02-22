/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.connector.dataplane.framework.pipeline;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure.Reason.GENERAL_ERROR;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure.Reason.NOT_FOUND;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PipelineServiceImplTest {

    Monitor monitor = mock();
    PipelineServiceImpl service = new PipelineServiceImpl(monitor);
    DataFlowStartMessage request = DataFlowStartMessage.Builder.newInstance()
            .id("1")
            .processId("1")
            .sourceDataAddress(DataAddress.Builder.newInstance().type("test").build())
            .destinationDataAddress(DataAddress.Builder.newInstance().type("test").build())
            .build();
    DataSourceFactory sourceFactory = mock(DataSourceFactory.class);
    DataSinkFactory sinkFactory = mock(DataSinkFactory.class);
    DataSource source = mock(DataSource.class);
    DataSink sink = mock(DataSink.class);

    @BeforeEach
    void setUp() {
        service.registerFactory(sourceFactory);
        service.registerFactory(sinkFactory);
    }

    @Test
    void transfer_invokesSink() {
        when(sourceFactory.canHandle(request)).thenReturn(true);
        when(sourceFactory.createSource(request)).thenReturn(source);
        when(sinkFactory.canHandle(request)).thenReturn(true);
        when(sinkFactory.createSink(request)).thenReturn(sink);
        when(sink.transfer(source)).thenReturn(completedFuture(StreamResult.success()));

        service.transfer(request);

        verify(sink).transfer(eq(source));
    }

    @Test
    void transfer_withCustomSink_shouldNotInvokeSinkFactory() throws Exception {
        var flowRequest = DataFlow.Builder.newInstance().id("dataFlowId")
                .source(DataAddress.Builder.newInstance().type("source").build())
                .destination(DataAddress.Builder.newInstance().type("custom-destination").build())
                .build()
                .toRequest();

        when(sourceFactory.canHandle(any())).thenReturn(true);
        when(sourceFactory.createSource(any())).thenReturn(source);

        var customSink = new DataSink() {
            @Override
            public CompletableFuture<StreamResult<Object>> transfer(DataSource source) {
                return CompletableFuture.completedFuture(StreamResult.success("test-response"));
            }
        };
        var future = service.transfer(flowRequest, customSink);

        assertThat(future).succeedsWithin(Duration.ofSeconds(5))
                .satisfies(res -> assertThat(res).isSucceeded().satisfies(obj -> assertThat(obj).isEqualTo("test-response")));

        verify(sourceFactory).createSource(flowRequest);
        verifyNoInteractions(sinkFactory);
    }

    @Test
    void terminate_shouldCloseDataSource() throws Exception {
        var dataFlow = DataFlow.Builder.newInstance().id("dPIataFlowId")
                .source(DataAddress.Builder.newInstance().type("source").build())
                .destination(DataAddress.Builder.newInstance().type("destination").build())
                .build();
        when(sourceFactory.canHandle(any())).thenReturn(true);
        when(sourceFactory.createSource(any())).thenReturn(source);
        when(sinkFactory.canHandle(any())).thenReturn(true);
        when(sinkFactory.createSink(any())).thenReturn(sink);
        when(sink.transfer(any())).thenReturn(completedFuture(StreamResult.success()));

        var future = service.transfer(dataFlow.toRequest()).thenApply(result -> service.terminate(dataFlow));

        assertThat(future).succeedsWithin(5, TimeUnit.SECONDS).satisfies(result -> {
            assertThat(result).isSucceeded();
        });
        verify(source).close();
    }

    @Test
    void terminate_shouldFail_whenSourceClosureFails() throws Exception {
        var dataFlow = DataFlow.Builder.newInstance().id("dataFlowId")
                .source(DataAddress.Builder.newInstance().type("source").build())
                .destination(DataAddress.Builder.newInstance().type("destination").build())
                .build();
        when(sourceFactory.canHandle(any())).thenReturn(true);
        when(sourceFactory.createSource(any())).thenReturn(source);
        when(sinkFactory.canHandle(any())).thenReturn(true);
        when(sinkFactory.createSink(any())).thenReturn(sink);
        when(sink.transfer(any())).thenReturn(completedFuture(StreamResult.success()));
        doThrow(IOException.class).when(source).close();

        var future = service.transfer(dataFlow.toRequest()).thenApply(result -> service.terminate(dataFlow));

        assertThat(future).succeedsWithin(5, TimeUnit.SECONDS).satisfies(result -> {
            assertThat(result).isFailed().extracting(StreamFailure::getReason).isEqualTo(GENERAL_ERROR);
        });
    }

    @Test
    void terminate_shouldFail_whenTransferDoesNotExist() {
        var dataFlow = DataFlow.Builder.newInstance().id("dataFlowId")
                .source(DataAddress.Builder.newInstance().type("source").build())
                .destination(DataAddress.Builder.newInstance().type("destination").build())
                .build();

        var result = service.terminate(dataFlow);

        assertThat(result).isFailed().extracting(StreamFailure::getReason).isEqualTo(NOT_FOUND);
        verifyNoInteractions(source);
    }

    @ParameterizedTest
    @ArgumentsSource(CanHandleArguments.class)
    void canHandle_returnsTrue_onlyIfSourceAndSinkCanHandle(
            boolean sourceFactoryResponse,
            boolean sinkFactoryResponse,
            boolean expectedResult
    ) {
        when(sourceFactory.canHandle(request)).thenReturn(sourceFactoryResponse);
        when(sinkFactory.canHandle(request)).thenReturn(sinkFactoryResponse);

        assertThat(service.canHandle(request))
                .isEqualTo(expectedResult);
    }

    private static class CanHandleArguments implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    arguments(true, true, true),
                    arguments(true, false, false),
                    arguments(false, true, false),
                    arguments(false, false, false)
            );
        }
    }
}
