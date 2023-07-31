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

package org.eclipse.edc.connector.dataplane.framework.e2e;

import org.eclipse.edc.connector.api.client.spi.transferprocess.NoopTransferProcessClient;
import org.eclipse.edc.connector.dataplane.framework.manager.DataPlaneManagerImpl;
import org.eclipse.edc.connector.dataplane.framework.pipeline.PipelineServiceImpl;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.edc.connector.dataplane.util.sink.OutputStreamDataSink;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Executors;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.mock;

public class EndToEndTest {

    public static DataFlowRequest.Builder createRequest(String type) {
        return DataFlowRequest.Builder.newInstance()
                .id("1")
                .processId("1")
                .sourceDataAddress(DataAddress.Builder.newInstance().type(type).build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type(type).build());
    }

    @Test
    void testEndToEnd() throws Exception {
        var monitor = mock(Monitor.class);

        var pipelineService = new PipelineServiceImpl(monitor);
        var endpoint = new FixedEndpoint(monitor);
        pipelineService.registerFactory(endpoint);

        var manager = DataPlaneManagerImpl.Builder.newInstance()
                .monitor(monitor)
                .pipelineService(pipelineService)
                .executorInstrumentation(ExecutorInstrumentation.noop())
                .transferProcessClient(new NoopTransferProcessClient())
                .build();
        manager.start();
        manager.transfer(new InputStreamDataSource("test", new ByteArrayInputStream("bytes".getBytes())), createRequest("1").build()).get();
    }

    private static class FixedEndpoint implements DataSinkFactory {
        private final ByteArrayOutputStream stream;
        private final OutputStreamDataSink sink;

        FixedEndpoint(Monitor monitor) {
            stream = new ByteArrayOutputStream();
            sink = new OutputStreamDataSink(randomUUID().toString(), stream, Executors.newFixedThreadPool(1), monitor);
        }

        public ByteArrayOutputStream getStream() {
            return stream;
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

}
