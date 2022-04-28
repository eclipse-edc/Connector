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

package org.eclipse.dataspaceconnector.dataplane.spi.pipeline;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParallelSinkTest {

    Faker faker = new Faker();
    Monitor monitor = mock(Monitor.class);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    String dataSourceName = faker.lorem().word();
    String dataSourceContent = faker.lorem().characters();
    String errorMessage = faker.lorem().sentence();

    FakeParallelSink fakeSink;

    @BeforeEach
    void setup() {
        fakeSink = new FakeParallelSink();
        fakeSink.monitor = monitor;
        fakeSink.telemetry = new Telemetry(); // default noop implementation
        fakeSink.executorService = executor;
        fakeSink.requestId = UUID.randomUUID().toString();
    }

    @Test
    void transfer_succeeds() {
        var dataSource = new InputStreamDataSource(dataSourceName, new ByteArrayInputStream(dataSourceContent.getBytes()));

        assertThat(fakeSink.transfer(dataSource)).succeedsWithin(500, TimeUnit.MILLISECONDS)
                .satisfies(transferResult -> assertThat(transferResult.succeeded()).isTrue());

        assertThat(fakeSink.parts).containsExactly(dataSource);
    }

    @Test
    void transfer_whenExceptionOpeningPartStream_fails() {
        var dataSourceMock = mock(DataSource.class);

        when(dataSourceMock.openPartStream()).thenThrow(new RuntimeException(errorMessage));

        assertThat(fakeSink.transfer(dataSourceMock)).succeedsWithin(500, TimeUnit.MILLISECONDS)
            .satisfies(transferResult -> assertThat(transferResult.failed()).isTrue())
            .satisfies(transferResult -> assertThat(transferResult.getFailureMessages()).containsExactly("Error processing data transfer request"));
    }

    @Test
    void transfer_whenFailureDuringTransfer_fails() {
        fakeSink.transferResultSupplier = () -> StatusResult.failure(ResponseStatus.FATAL_ERROR, errorMessage);

        var dataSource = new InputStreamDataSource(dataSourceName, new ByteArrayInputStream(dataSourceContent.getBytes()));

        assertThat(fakeSink.transfer(dataSource)).succeedsWithin(500, TimeUnit.MILLISECONDS)
            .satisfies(transferResult -> assertThat(transferResult.failed()).isTrue())
                .satisfies(transferResult -> assertThat(transferResult.getFailure().status()).isEqualTo(ResponseStatus.ERROR_RETRY))
            .satisfies(transferResult -> assertThat(transferResult.getFailureMessages()).containsExactly(errorMessage));

        assertThat(fakeSink.parts).containsExactly(dataSource);
    }

    @Test
    void transfer_whenExceptionDuringTransfer_fails() {
        fakeSink.transferResultSupplier = () -> {
            throw new RuntimeException(errorMessage);
        };
        var dataSource = new InputStreamDataSource(dataSourceName, new ByteArrayInputStream(dataSourceContent.getBytes()));

        assertThat(fakeSink.transfer(dataSource)).succeedsWithin(500, TimeUnit.MILLISECONDS)
                .satisfies(transferResult -> assertThat(transferResult.failed()).isTrue())
                .satisfies(transferResult -> assertThat(transferResult.getFailure().status()).isEqualTo(ResponseStatus.ERROR_RETRY))
                .satisfies(transferResult -> assertThat(transferResult.getFailureMessages())
                        .containsExactly("Unhandled exception raised when transferring data: java.lang.RuntimeException: " + errorMessage));

        assertThat(fakeSink.parts).containsExactly(dataSource);
    }

    private static class FakeParallelSink extends ParallelSink {

        List<DataSource.Part> parts;
        Supplier<StatusResult<Void>> transferResultSupplier = StatusResult::success;

        @Override
        protected StatusResult<Void> transferParts(List<DataSource.Part> parts) {
            this.parts = parts;
            return transferResultSupplier.get();
        }
    }
}
