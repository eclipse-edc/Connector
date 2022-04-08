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

package org.eclipse.dataspaceconnector.dataplane.framework.pipeline;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PipelineServiceImplTest {
    PipelineServiceImpl service = new PipelineServiceImpl();
    DataFlowRequest request = DataFlowRequest.Builder.newInstance()
            .id("1")
            .processId("1")
            .sourceDataAddress(DataAddress.Builder.newInstance().type("test").build())
            .destinationDataAddress(DataAddress.Builder.newInstance().type("test").build())
            .build();
    DataSourceFactory sourceFactory = mock(DataSourceFactory.class);
    DataSinkFactory sinkFactory = mock(DataSinkFactory.class);
    DataSource source = mock(DataSource.class);
    DataSink sink = mock(DataSink.class);

    {
        service.registerFactory(sourceFactory);
        service.registerFactory(sinkFactory);
    }

    @Test
    void transfer_invokesSink() {
        when(sourceFactory.canHandle(request)).thenReturn(true);
        when(sourceFactory.createSource(request)).thenReturn(source);
        when(sinkFactory.canHandle(request)).thenReturn(true);
        when(sinkFactory.createSink(request)).thenReturn(sink);
        when(sink.transfer(source)).thenReturn(completedFuture(StatusResult.success()));

        service.transfer(request);

        verify(sink).transfer(eq(source));
    }

    @ParameterizedTest
    @MethodSource("canHandleArguments")
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

    private static Stream<Arguments> canHandleArguments() {
        return Stream.of(
                arguments(true, true, true),
                arguments(true, false, false),
                arguments(false, true, false),
                arguments(false, false, false)
        );
    }
}
