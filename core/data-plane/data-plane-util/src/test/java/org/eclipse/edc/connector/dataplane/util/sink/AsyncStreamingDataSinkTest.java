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
import org.eclipse.edc.connector.dataplane.util.sink.AsyncStreamingDataSink.AsyncResponseContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsyncStreamingDataSinkTest {
    private static final byte[] TEST_CONTENT = "test".getBytes();

    private AsyncResponseContext asyncContext;
    private ExecutorService executorService;
    private Monitor monitor;

    @Test
    void verify_streaming() throws Exception {
        var part = mock(DataSource.Part.class);
        when(part.openStream()).thenReturn(new ByteArrayInputStream(TEST_CONTENT));

        var dataSource = mock(DataSource.class);
        when(dataSource.openPartStream()).thenReturn(Stream.of(part));

        var dataSink = new AsyncStreamingDataSink(asyncContext, executorService, monitor);

        var outputStream = new ByteArrayOutputStream();

        //noinspection unchecked
        when(asyncContext.register(isA(Consumer.class))).thenAnswer((Answer<Boolean>) invocation -> {
            @SuppressWarnings("rawtypes") var consumer = (Consumer) invocation.getArgument(0);
            //noinspection unchecked
            consumer.accept(outputStream);
            return true;
        });

        var future = dataSink.transfer(dataSource);

        var result = future.get(2000, MILLISECONDS);

        assertThat(result.succeeded()).isTrue();

        assertThat(outputStream.toByteArray()).isEqualTo(TEST_CONTENT);
    }

    @Test
    void verify_exceptionThrown() throws Exception {
        var part = mock(DataSource.Part.class);
        when(part.openStream()).thenReturn(new ByteArrayInputStream(TEST_CONTENT));

        var dataSource = mock(DataSource.class);
        when(dataSource.openPartStream()).thenReturn(Stream.of(part));

        var dataSink = new AsyncStreamingDataSink(asyncContext, executorService, monitor);

        var outputStream = mock(OutputStream.class);

        var testException = new RuntimeException("Test Exception");

        doThrow(testException).when(outputStream).write(isA(byte[].class), anyInt(), anyInt());

        //noinspection unchecked
        when(asyncContext.register(isA(Consumer.class))).thenAnswer((Answer<Boolean>) invocation -> {
            @SuppressWarnings("rawtypes") var consumer = (Consumer) invocation.getArgument(0);
            //noinspection unchecked
            consumer.accept(outputStream);
            return true;
        });

        var future = dataSink.transfer(dataSource);

        assertThatThrownBy(() -> future.get(2000, MILLISECONDS)).hasCause(testException);
    }

    @BeforeEach
    void setUp() {
        asyncContext = mock(AsyncResponseContext.class);
        executorService = newSingleThreadExecutor();
        monitor = mock(Monitor.class);
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}
