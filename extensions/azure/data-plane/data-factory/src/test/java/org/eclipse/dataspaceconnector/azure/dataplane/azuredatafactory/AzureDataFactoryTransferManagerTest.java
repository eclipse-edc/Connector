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

package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory;

import com.azure.resourcemanager.datafactory.models.CreateRunResponse;
import com.azure.resourcemanager.datafactory.models.PipelineResource;
import com.azure.resourcemanager.datafactory.models.PipelineRun;
import com.github.javafaker.Faker;
import org.assertj.core.api.ObjectAssert;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureDataFactoryTransferManagerTest {
    static final Faker FAKER = new Faker();

    Monitor monitor = mock(Monitor.class);
    Clock clock = mock(Clock.class);
    Instant fixedInstant = Instant.EPOCH;
    DataFactoryClient client = mock(DataFactoryClient.class);
    DataFactoryPipelineFactory pipelineFactory = mock(DataFactoryPipelineFactory.class);
    Duration maxDuration = Duration.ofMillis(FAKER.number().numberBetween(1, 10));
    AzureDataFactoryTransferManager transferManager = new AzureDataFactoryTransferManager(
            monitor,
            client,
            pipelineFactory,
            maxDuration,
            clock,
            Duration.ofMillis(0));

    DataFlowRequest request = AzureDataFactoryTransferRequestValidatorTest.requestWithProperties;

    PipelineResource pipeline = mock(PipelineResource.class);
    CreateRunResponse runResponse = mock(CreateRunResponse.class);
    String runId = FAKER.lorem().characters();
    PipelineRun run = mock(PipelineRun.class);

    @BeforeEach
    void setUp() {
        when(clock.instant())
                .thenReturn(fixedInstant);
        when(pipelineFactory.createPipeline(request))
                .thenReturn(pipeline);
        when(client.runPipeline(pipeline))
                .thenReturn(runResponse);
        when(runResponse.runId())
                .thenReturn(runId);
        when(client.getPipelineRun(runId)).thenReturn(run);
    }

    @ParameterizedTest
    @MethodSource("successStates")
    void transfer_success(List<String> states) {
        // Arrange
        when(run.status()).thenReturn(states.get(0), states.stream().skip(1).toArray(String[]::new));

        // Act & Assert
        assertThatTransferResult()
                .matches(StatusResult::succeeded);
    }

    static Stream<Arguments> successStates() {
        return Stream.of(
                arguments(List.of("Succeeded")),
                arguments(List.of("Queued", "InProgress", "Succeeded"))
        );
    }

    @ParameterizedTest
    @MethodSource("failureStates")
    void transfer_failure(List<String> states) {
        // Arrange
        when(run.status()).thenReturn(states.get(0), states.stream().skip(1).toArray(String[]::new));

        // Act & Assert
        assertThatTransferResult()
                .matches(StatusResult::failed);
    }

    static Stream<Arguments> failureStates() {
        return Stream.of(
                arguments(List.of("Failed")),
                arguments(List.of("Queued", "InProgress", "Failed")),
                arguments(List.of("Queued", "InProgress", "Canceling", "Cancelled")),
                arguments(List.of("Cancelled")),
                arguments(List.of("Queued", "InProgress", FAKER.lorem().word()))
        );
    }

    @Test
    void transfer_timeout() {
        // Arrange
        when(clock.instant())
                .thenReturn(fixedInstant, fixedInstant, fixedInstant.plus(maxDuration));
        when(run.status()).thenReturn("InProgress");

        // Act & Assert
        assertThatTransferResult()
                .satisfies(r -> {
                    assertThat(r.failed()).isTrue();
                    assertThat(r.getFailureMessages()).containsOnly("ADF run timed out");
                });
        verify(client).cancelPipelineRun(runId);
    }

    ObjectAssert<StatusResult<Void>> assertThatTransferResult() {
        return assertThat(transferManager.transfer(request))
                .succeedsWithin(Duration.ofMinutes(1));
    }
}
