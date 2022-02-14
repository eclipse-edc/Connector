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
package org.eclipse.dataspaceconnector.dataplane.framework.e2e;

import org.eclipse.dataspaceconnector.dataplane.framework.manager.DataPlaneManagerImpl;
import org.eclipse.dataspaceconnector.dataplane.framework.pipeline.PipelineServiceImpl;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSink;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;

public class EndToEndTest {

    @Test
    void testEndToEnd() throws Exception {
        var monitor = mock(Monitor.class);

        var pipelineService = new PipelineServiceImpl();
        var endpoint = new FixedEndpoint(monitor);
        pipelineService.registerFactory(endpoint);

        var manager = DataPlaneManagerImpl.Builder.newInstance()
                .monitor(monitor)
                .pipelineService(pipelineService)
                .build();
        manager.start();
        manager.transfer(new InputStreamDataSource("test", new ByteArrayInputStream("bytes".getBytes())), createRequest("1").build()).get();
    }

    private static class FixedEndpoint implements DataSinkFactory {
        private final ByteArrayOutputStream stream;
        private final OutputStreamDataSink sink;

        public FixedEndpoint(Monitor monitor) {
            stream = new ByteArrayOutputStream();
            sink = new OutputStreamDataSink(stream, Executors.newFixedThreadPool(1), monitor);
        }

        public ByteArrayOutputStream getStream() {
            return stream;
        }

        @Override
        public boolean canHandle(DataFlowRequest request) {
            return true;
        }

        @Override
        public @NotNull Result<Boolean> validate(DataFlowRequest request) {
            return VALID;
        }

        @Override
        public DataSink createSink(DataFlowRequest request) {
            return sink;
        }
    }

    public static DataFlowRequest.Builder createRequest(String type) {
        return DataFlowRequest.Builder.newInstance()
                .id("1")
                .processId("1")
                .sourceDataAddress(DataAddress.Builder.newInstance().type(type).build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type(type).build());
    }

}
