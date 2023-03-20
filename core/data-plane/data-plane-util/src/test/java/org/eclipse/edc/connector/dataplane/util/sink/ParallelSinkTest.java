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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.connector.dataplane.util.sink;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParallelSinkTest {

    private final Monitor monitor = mock(Monitor.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final String dataSourceName = "test-datasource-name";
    private final String dataSourceContent = "test-content";
    private final String errorMessage = "test-errormessage";
    private final InputStreamDataSource dataSource = new InputStreamDataSource(
            dataSourceName,
            new ByteArrayInputStream(dataSourceContent.getBytes()));
    private final String dataFlowRequestId = randomUUID().toString();
    FakeParallelSink fakeSink;

    @BeforeEach
    void setup() {
        fakeSink = new FakeParallelSink();
        fakeSink.monitor = monitor;
        fakeSink.telemetry = new Telemetry(); // default noop implementation
        fakeSink.executorService = executor;
        fakeSink.requestId = dataFlowRequestId;
    }

    @Test
    void transfer_succeeds() {
        assertThat(fakeSink.transfer(dataSource)).succeedsWithin(500, TimeUnit.MILLISECONDS)
                .satisfies(transferResult -> assertThat(transferResult.succeeded()).isTrue());

        Assertions.assertThat(fakeSink.parts).containsExactly(dataSource);
        assertThat(fakeSink.complete).isEqualTo(1);
    }

    @Test
    void transfer_whenCompleteFails_fails() {
        fakeSink.completeResponse = StatusResult.failure(ResponseStatus.ERROR_RETRY);
        assertThat(fakeSink.transfer(dataSource)).succeedsWithin(500, TimeUnit.MILLISECONDS)
                .isEqualTo(fakeSink.completeResponse);
    }

    @Test
    void transfer_whenExceptionOpeningPartStream_fails() {
        var dataSourceMock = mock(DataSource.class);

        when(dataSourceMock.openPartStream()).thenThrow(new RuntimeException(errorMessage));

        assertThat(fakeSink.transfer(dataSourceMock)).succeedsWithin(500, TimeUnit.MILLISECONDS)
                .satisfies(transferResult -> assertThat(transferResult.failed()).isTrue())
                .satisfies(transferResult -> assertThat(transferResult.getFailureMessages()).containsExactly(format("Error processing data transfer request - Request ID: %s", dataFlowRequestId)));
        assertThat(fakeSink.complete).isEqualTo(0);
    }

    @Test
    void transfer_whenFailureDuringTransfer_fails() {
        fakeSink.transferResultSupplier = () -> StatusResult.failure(ResponseStatus.FATAL_ERROR, errorMessage);

        assertThat(fakeSink.transfer(dataSource)).succeedsWithin(500, TimeUnit.MILLISECONDS)
                .satisfies(transferResult -> assertThat(transferResult.failed()).isTrue())
                .satisfies(transferResult -> assertThat(transferResult.getFailure().status()).isEqualTo(ResponseStatus.ERROR_RETRY))
                .satisfies(transferResult -> assertThat(transferResult.getFailureMessages()).containsExactly(errorMessage));

        assertThat(fakeSink.parts).containsExactly(dataSource);
        assertThat(fakeSink.complete).isEqualTo(0);
    }

    @Test
    void transfer_whenExceptionDuringTransfer_fails() {
        fakeSink.transferResultSupplier = () -> {
            throw new RuntimeException(errorMessage);
        };

        assertThat(fakeSink.transfer(dataSource)).succeedsWithin(500, TimeUnit.MILLISECONDS)
                .satisfies(transferResult -> assertThat(transferResult.failed()).isTrue())
                .satisfies(transferResult -> assertThat(transferResult.getFailure().status()).isEqualTo(ResponseStatus.ERROR_RETRY))
                .satisfies(transferResult -> assertThat(transferResult.getFailureMessages())
                        .containsExactly("Unhandled exception raised when transferring data: java.lang.RuntimeException: " + errorMessage));

        assertThat(fakeSink.parts).containsExactly(dataSource);
        assertThat(fakeSink.complete).isEqualTo(0);
    }

    private static class FakeParallelSink extends ParallelSink {

        List<DataSource.Part> parts;
        Supplier<StatusResult<Void>> transferResultSupplier = StatusResult::success;
        private int complete;
        private StatusResult<Void> completeResponse = StatusResult.success();

        @Override
        protected StatusResult<Void> transferParts(List<DataSource.Part> parts) {
            this.parts = parts;
            return transferResultSupplier.get();
        }

        @Override
        protected StatusResult<Void> complete() {
            complete++;
            return completeResponse;
        }
    }
}
