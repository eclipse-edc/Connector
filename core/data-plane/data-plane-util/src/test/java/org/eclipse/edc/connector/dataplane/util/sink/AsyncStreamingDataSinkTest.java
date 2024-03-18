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

package org.eclipse.edc.connector.dataplane.util.sink;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.util.sink.AsyncStreamingDataSink.AsyncResponseContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsyncStreamingDataSinkTest {
    private static final byte[] TEST_CONTENT = "test".getBytes();

    private final AsyncResponseContext asyncContext = mock();
    private final ExecutorService executorService = newSingleThreadExecutor();
    private final Monitor monitor = mock();

    private final AsyncStreamingDataSink dataSink = new AsyncStreamingDataSink(asyncContext, executorService);

    @Test
    void verify_streaming() {
        var part = mock(DataSource.Part.class);
        when(part.openStream()).thenReturn(new ByteArrayInputStream(TEST_CONTENT));

        var dataSource = mock(DataSource.class);
        when(dataSource.openPartStream()).thenReturn(success(Stream.of(part)));

        var outputStream = new ByteArrayOutputStream();

        //noinspection unchecked
        when(asyncContext.register(isA(AsyncStreamingDataSink.AsyncResponseCallback.class))).thenAnswer((Answer<Boolean>) invocation -> {
            @SuppressWarnings("rawtypes") var callback = (AsyncStreamingDataSink.AsyncResponseCallback) invocation.getArgument(0);
            callback.outputStreamConsumer().accept(outputStream);
            return true;
        });

        var future = dataSink.transfer(dataSource);

        assertThat(future).succeedsWithin(2, SECONDS).satisfies(result -> {
            assertThat(result).isSucceeded();
            assertThat(outputStream.toByteArray()).isEqualTo(TEST_CONTENT);
        });
    }

    @Test
    void verify_exceptionThrown() throws Exception {
        var part = mock(DataSource.Part.class);
        when(part.openStream()).thenReturn(new ByteArrayInputStream(TEST_CONTENT));

        var dataSource = mock(DataSource.class);
        when(dataSource.openPartStream()).thenReturn(StreamResult.success(Stream.of(part)));

        var outputStream = mock(OutputStream.class);

        var testException = new RuntimeException("Test Exception");

        doThrow(testException).when(outputStream).write(isA(byte[].class), anyInt(), anyInt());

        //noinspection unchecked
        when(asyncContext.register(isA(AsyncStreamingDataSink.AsyncResponseCallback.class))).thenAnswer((Answer<Boolean>) invocation -> {
            @SuppressWarnings("rawtypes") var callback = (AsyncStreamingDataSink.AsyncResponseCallback) invocation.getArgument(0);
            callback.outputStreamConsumer().accept(outputStream);
            return true;
        });

        var future = dataSink.transfer(dataSource);

        assertThat(future).failsWithin(2, SECONDS).withThrowableThat().havingCause().isEqualTo(testException);
    }

}
