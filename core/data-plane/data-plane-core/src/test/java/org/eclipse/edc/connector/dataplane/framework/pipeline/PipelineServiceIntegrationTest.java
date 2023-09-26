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
import org.eclipse.edc.connector.dataplane.util.sink.OutputStreamDataSink;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
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
        public @NotNull Result<Void> validateRequest(DataFlowRequest request) {
            return Result.success();
        }

        @Override
        public DataSink createSink(DataFlowRequest request) {
            return sink;
        }
    }

    private static class InputStreamDataFactory implements DataSourceFactory {
        @Override
        public boolean canHandle(DataFlowRequest request) {
            return true;
        }

        @Override
        public DataSource createSource(DataFlowRequest request) {
            return new InputStreamDataSource("test", new ByteArrayInputStream("bytes".getBytes()));
        }

        @Override
        public @NotNull Result<Void> validateRequest(DataFlowRequest request) {
            return Result.success();
        }
    }
}
