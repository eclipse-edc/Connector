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

package org.eclipse.dataspaceconnector.transfer.core.transfer;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.retry.WaitStrategy;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.REQUESTING;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferProcessSendRetryManagerTest {
    static final String DESTINATION_TYPE = "test-type";
    static Faker faker = new Faker();

    final Monitor monitor = new ConsoleMonitor();
    final WaitStrategy delayStrategy = mock(WaitStrategy.class);
    final int sendRetryLimit = faker.number().numberBetween(5, 10);

    final Clock clock = mock(Clock.class);
    final TransferProcessSendRetryManager sendRetryManager =
            new TransferProcessSendRetryManager(monitor, () -> delayStrategy, clock, sendRetryLimit);

    @ParameterizedTest
    @MethodSource("delayArgs")
    void shouldDelay(long stateTimestamp, long currentTime, long retryDelay, boolean shouldDelay) {
        var stateCount = sendRetryLimit - 2;
        var process = TransferProcess.Builder.newInstance()
                .type(TransferProcess.Type.CONSUMER)
                .state(REQUESTING.code())
                .id(UUID.randomUUID().toString())
                .stateCount(stateCount)
                .stateTimestamp(stateTimestamp)
                .dataRequest(DataRequest.Builder.newInstance().destinationType(DESTINATION_TYPE).build())
                .build();

        when(delayStrategy.retryInMillis())
                .thenAnswer(i -> {
                    verify(delayStrategy).failures(stateCount - 1);
                    return retryDelay;
                }).thenThrow(new RuntimeException("should call only once"));

        when(clock.millis()).thenReturn(currentTime);

        assertThat(sendRetryManager.shouldDelay(process))
                .isEqualTo(shouldDelay);
    }

    static Stream<Arguments> delayArgs() {
        return Stream.of(
                arguments(0, 0, 0, false),
                arguments(0, 10, 9, false),
                arguments(0, 9, 10, true),
                arguments(2, 10, 9, true),
                arguments(2, 12, 9, false)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -1, 0, 1, 2})
    void retriesExhausted(int retriesLeft) {
        var stateCount = sendRetryLimit - retriesLeft;
        var stateTimestamp = faker.number().randomNumber();
        var process = TransferProcess.Builder.newInstance()
                .type(TransferProcess.Type.CONSUMER)
                .state(REQUESTING.code())
                .id(UUID.randomUUID().toString())
                .stateCount(stateCount)
                .stateTimestamp(stateTimestamp)
                .dataRequest(DataRequest.Builder.newInstance().destinationType(DESTINATION_TYPE).build())
                .build();

        var expected = retriesLeft < 0;
        assertThat(sendRetryManager.retriesExhausted(process))
                .isEqualTo(expected);
    }
}