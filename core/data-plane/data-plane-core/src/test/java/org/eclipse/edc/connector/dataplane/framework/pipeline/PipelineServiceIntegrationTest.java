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

package org.eclipse.edc.connector.dataplane.framework.pipeline;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.util.sink.OutputStreamDataSink;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.UUID.randomUUID;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.failure;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.util.async.AsyncUtils.asyncAllOf;
import static org.mockito.Mockito.mock;

public class PipelineServiceIntegrationTest {

    private final Monitor monitor = mock();

    @Test
    void transferData() {
        var pipelineService = new PipelineServiceImpl(monitor);
        var endpoint = new FixedEndpoint(monitor);
        pipelineService.registerFactory(endpoint);
        pipelineService.registerFactory(new InputStreamDataFactory());

        var future = pipelineService.transfer(createRequest().build());

        assertThat(future).succeedsWithin(5, TimeUnit.SECONDS).satisfies(result -> {
            assertThat(result).isSucceeded().isEqualTo("bytes");
        });
    }

    @Test
    void transferData_withCustomSink() {
        var pipelineService = new PipelineServiceImpl(monitor);
        var text = "test-data-input-transferred-to-a-memory-stream";
        pipelineService.registerFactory(new InputStreamDataFactory(text));

        var future = pipelineService.transfer(createRequest().build(), new MemorySink());

        assertThat(future).succeedsWithin(5, TimeUnit.SECONDS).satisfies(result -> {
            assertThat(result).isSucceeded().isInstanceOf(byte[].class);
            var bytes = (byte[]) result.getContent();
            assertThat(bytes).isEqualTo(text.getBytes());
        });
    }

    private DataFlowRequest.Builder createRequest() {
        return DataFlowRequest.Builder.newInstance()
                .id("1")
                .processId("1")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("any").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("any").build());
    }

    private static class FixedEndpoint implements DataSinkFactory {
        private final OutputStreamDataSink sink;

        FixedEndpoint(Monitor monitor) {
            sink = new OutputStreamDataSink(randomUUID().toString(), Executors.newFixedThreadPool(1), monitor);
        }

        @Override
        public boolean canHandle(DataFlowRequest request) {
            return true;
        }

        @Override
        public DataSink createSink(DataFlowRequest request) {
            return sink;
        }

        @Override
        public @NotNull Result<Void> validateRequest(DataFlowRequest request) {
            return Result.success();
        }
    }

    private static class InputStreamDataFactory implements DataSourceFactory {
        private final String data;

        InputStreamDataFactory(String text) {
            this.data = text;
        }

        InputStreamDataFactory() {
            this("bytes");
        }

        @Override
        public boolean canHandle(DataFlowRequest request) {
            return true;
        }

        @Override
        public DataSource createSource(DataFlowRequest request) {
            return new InputStreamDataSource("test", new ByteArrayInputStream(data.getBytes()));
        }

        @Override
        public @NotNull Result<Void> validateRequest(DataFlowRequest request) {
            return Result.success();
        }
    }

    private static class MemorySink implements DataSink {

        private final ByteArrayOutputStream bos;

        MemorySink() {
            bos = new ByteArrayOutputStream();
        }

        @Override
        public CompletableFuture<StreamResult<Object>> transfer(DataSource source) {
            var streamResult = source.openPartStream();
            if (streamResult.failed()) {
                return completedFuture(failure(streamResult.getFailure()));
            }
            var partStream = streamResult.getContent();
            return partStream
                    .map(part -> supplyAsync(() -> transferTo(part, bos)))
                    .collect(asyncAllOf())
                    .thenApply(longs -> success(bos.toByteArray()));
        }

        private long transferTo(DataSource.Part part, OutputStream stream) {
            try {
                return part.openStream().transferTo(stream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
