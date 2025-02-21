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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure.Reason.GENERAL_ERROR;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure.Reason.NOT_FOUND;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PipelineServiceImplTest {

    private static final String PROCESS_ID = "1";

    private final Monitor monitor = mock();

    private final DataSourceFactory sourceFactory = mock();

    private final DataSinkFactory sinkFactory = mock();
    private final DataSource source = mock();
    private final DataSink sink = mock();
    private final PipelineServiceImpl service = new PipelineServiceImpl(monitor);

    @BeforeEach
    void setUp() {
        service.registerFactory(sourceFactory);
        service.registerFactory(sinkFactory);
    }

    @Nested
    class Transfer {
        @Test
        void transfer_invokesSink() throws Exception {
            when(sourceFactory.supportedType()).thenReturn("source");
            when(sourceFactory.createSource(any())).thenReturn(source);
            when(sinkFactory.supportedType()).thenReturn("destination");
            when(sinkFactory.createSink(any())).thenReturn(sink);
            when(sink.transfer(any())).thenReturn(completedFuture(StreamResult.success()));

            var future = service.transfer(dataFlow("source", "destination").toRequest());

            assertThat(future).succeedsWithin(5, TimeUnit.SECONDS).satisfies(result -> {
                assertThat(result).isSucceeded();
            });
            verify(sink).transfer(source);
            verify(source).close();
        }

        @Test
        void transfer_withCustomSink_shouldNotInvokeSinkFactory() throws Exception {
            var flowRequest = dataFlow("source", "custom-destination").toRequest();

            when(sourceFactory.supportedType()).thenReturn("source");
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
            verify(source).close();
        }

        @Test
        void transfer_withUnknownSource_shouldFail() {
            var flowRequest = dataFlow("wrong-source", "custom-destination").toRequest();
            var expectedErrorMessage = format("Unknown data source type wrong-source for flow id: %s.", PROCESS_ID);

            when(sourceFactory.supportedType()).thenReturn("source");
            when(sourceFactory.createSource(any())).thenReturn(source);

            var customSink = new DataSink() {
                @Override
                public CompletableFuture<StreamResult<Object>> transfer(DataSource source) {
                    return CompletableFuture.completedFuture(StreamResult.success("test-response"));
                }
            };

            var future = service.transfer(flowRequest, customSink);
            assertThat(future).succeedsWithin(Duration.ofSeconds(5))
                    .satisfies(res -> assertThat(res).isFailed())
                    .satisfies(res -> assertThat(res.getFailure().getMessages()).hasSize(1))
                    .satisfies(res -> assertThat(res.getFailure().getMessages().get(0)).isEqualTo(expectedErrorMessage));

            verify(sourceFactory).supportedType();
            verifyNoInteractions(sinkFactory);
            verifyNoInteractions(source);
        }

        @Test
        void transfer_withUnknownSink_shouldFail() {
            var flowRequest = dataFlow("source", "custom-destination").toRequest();
            var expectedErrorMessage = format("Unknown data sink type custom-destination for flow id: %s.", PROCESS_ID);

            when(sourceFactory.supportedType()).thenReturn("source");
            when(sourceFactory.createSource(any())).thenReturn(source);

            var future = service.transfer(flowRequest);
            assertThat(future).succeedsWithin(Duration.ofSeconds(5))
                    .satisfies(res -> assertThat(res).isFailed())
                    .satisfies(res -> assertThat(res.getFailure().getMessages()).hasSize(1))
                    .satisfies(res -> assertThat(res.getFailure().getMessages().get(0)).isEqualTo(expectedErrorMessage));

            verify(sinkFactory).supportedType();
            verifyNoInteractions(sourceFactory);
            verifyNoInteractions(source);
        }
    }

    @Nested
    class Terminate {
        @Test
        void shouldCloseDataSource() throws Exception {
            var dataFlow = dataFlow("source", "destination");
            when(sourceFactory.supportedType()).thenReturn("source");
            when(sourceFactory.createSource(any())).thenReturn(source);
            when(sinkFactory.supportedType()).thenReturn("destination");
            when(sinkFactory.createSink(any())).thenReturn(sink);
            when(sink.transfer(any())).thenReturn(new CompletableFuture<>());

            service.transfer(dataFlow.toRequest());

            var result = service.terminate(dataFlow);

            assertThat(result).isSucceeded();
            verify(source).close();
        }

        @Test
        void shouldFail_whenSourceClosureFails() throws Exception {
            var dataFlow = dataFlow("source", "destination");
            when(sourceFactory.supportedType()).thenReturn("source");
            when(sourceFactory.createSource(any())).thenReturn(source);
            when(sinkFactory.supportedType()).thenReturn("destination");
            when(sinkFactory.createSink(any())).thenReturn(sink);
            when(sink.transfer(any())).thenReturn(new CompletableFuture<>());
            doThrow(IOException.class).when(source).close();

            service.transfer(dataFlow.toRequest());

            var result = service.terminate(dataFlow);

            assertThat(result).isFailed().extracting(StreamFailure::getReason).isEqualTo(GENERAL_ERROR);
            verify(source).close();
        }

        @Test
        void shouldFail_whenTransferDoesNotExist() {
            var dataFlow = dataFlow("source", "destination");

            var result = service.terminate(dataFlow);

            assertThat(result).isFailed().extracting(StreamFailure::getReason).isEqualTo(NOT_FOUND);
            verifyNoInteractions(source);
        }
    }

    @ParameterizedTest
    @ArgumentsSource(CanHandleArguments.class)
    void canHandle_shouldReturnTrue_whenSourceAndDestinationCanBeHandled(String source, String destination, boolean expected) {
        when(sourceFactory.supportedType()).thenReturn("source");
        when(sinkFactory.supportedType()).thenReturn("destination");

        boolean result = service.canHandle(dataFlow(source, destination).toRequest());

        assertThat(result).isEqualTo(expected);
    }

    @Nested
    class SupportedTypes {

        @Test
        void shouldReturnSourceTypesFromFactories() {
            when(sourceFactory.supportedType()).thenReturn("source");

            var result = service.supportedSourceTypes();

            assertThat(result).containsOnly("source");
            verifyNoInteractions(sinkFactory);
        }

        @Test
        void shouldReturnSinkTypesFromFactories() {
            when(sinkFactory.supportedType()).thenReturn("sink");

            var result = service.supportedSinkTypes();

            assertThat(result).containsOnly("sink");
            verifyNoInteractions(sourceFactory);
        }

    }

    @Nested
    class CloseAll {

        @Test
        void shouldCloseAllTheOngoingDataFlows() throws Exception {
            when(sourceFactory.supportedType()).thenReturn("source");
            when(sourceFactory.createSource(any())).thenReturn(source);
            when(sinkFactory.supportedType()).thenReturn("destination");
            when(sinkFactory.createSink(any())).thenReturn(sink);
            when(sink.transfer(any())).thenReturn(new CompletableFuture<>());

            service.transfer(dataFlow("source", "destination").toRequest());

            service.closeAll();

            verify(source).close();
        }
    }

    private DataFlow dataFlow(String sourceType, String destinationType) {
        return DataFlow.Builder.newInstance()
                .id(PROCESS_ID)
                .source(DataAddress.Builder.newInstance().type(sourceType).build())
                .destination(DataAddress.Builder.newInstance().type(destinationType).build())
                .build();
    }

    private static class CanHandleArguments implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    arguments("source", "destination", true),
                    arguments("unsupported_source", "destination", false),
                    arguments("source", "unsupported_destination", false),
                    arguments("unsupported_source", "unsupported_destination", false)
            );
        }
    }
}
