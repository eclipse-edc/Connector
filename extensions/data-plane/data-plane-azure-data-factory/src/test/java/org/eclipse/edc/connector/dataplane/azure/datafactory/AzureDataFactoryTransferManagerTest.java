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

package org.eclipse.edc.connector.dataplane.azure.datafactory;

import com.azure.core.credential.AzureSasCredential;
import com.azure.resourcemanager.datafactory.models.CreateRunResponse;
import com.azure.resourcemanager.datafactory.models.PipelineResource;
import com.azure.resourcemanager.datafactory.models.PipelineRun;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.assertj.core.api.ObjectAssert;
import org.bouncycastle.util.test.UncloseableOutputStream;
import org.eclipse.edc.azure.blob.AzureSasToken;
import org.eclipse.edc.azure.blob.adapter.BlobAdapter;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureDataFactoryTransferManagerTest {

    private final Monitor monitor = mock(Monitor.class);
    private final Clock clock = mock(Clock.class);
    private final Instant fixedInstant = Instant.EPOCH;
    private final DataFactoryClient client = mock(DataFactoryClient.class);
    private final DataFactoryPipelineFactory pipelineFactory = mock(DataFactoryPipelineFactory.class);
    private final BlobStoreApi blobStoreApi = mock(BlobStoreApi.class);
    private final TypeManager typeManager = new TypeManager();
    private final KeyVaultClient keyVaultClient = mock(KeyVaultClient.class);
    private final KeyVaultSecret keyVaultSecret = mock(KeyVaultSecret.class);
    private final BlobAdapter blobAdapter = mock(BlobAdapter.class);
    private final DataFlowRequest request = TestFunctions.createFlowRequest();
    private final PipelineResource pipeline = mock(PipelineResource.class);
    private final CreateRunResponse runResponse = mock(CreateRunResponse.class);
    private final String runId = UUID.randomUUID().toString();
    private final PipelineRun run = mock(PipelineRun.class);
    private final Random random = new Random();
    private final AzureSasToken sasToken = new AzureSasToken("test-wo-sas", random.nextLong());
    private final Duration maxDuration = Duration.ofMillis(1 + random.nextInt(11));
    private final AzureDataFactoryTransferManager transferManager = new AzureDataFactoryTransferManager(
            monitor,
            client,
            pipelineFactory,
            maxDuration,
            clock,
            blobStoreApi,
            typeManager,
            keyVaultClient,
            Duration.ofMillis(0));

    static Stream<Arguments> successStates() {
        return Stream.of(
                arguments(List.of("Succeeded")),
                arguments(List.of("Queued", "InProgress", "Succeeded"))
        );
    }

    static Stream<Arguments> failureStates() {
        return Stream.of(
                arguments(List.of("Failed")),
                arguments(List.of("Queued", "InProgress", "Failed")),
                arguments(List.of("Queued", "InProgress", "Canceling", "Cancelled")),
                arguments(List.of("Cancelled")),
                arguments(List.of("Queued", "InProgress", UUID.randomUUID().toString()))
        );
    }

    @BeforeEach
    void setUp() {
        when(clock.instant())
                .thenReturn(fixedInstant);
        when(pipelineFactory.createPipeline(request))
                .thenReturn(pipeline);
        when(client.runPipeline(pipeline))
                .thenReturn(runResponse);
        when(keyVaultClient.getSecret(any()))
                .thenReturn(keyVaultSecret);
        when(keyVaultSecret.getValue())
                .thenReturn(typeManager.writeValueAsString(sasToken));
        when(blobStoreApi.getBlobAdapter(any(), any(), any(), any(AzureSasCredential.class)))
                .thenReturn(blobAdapter);
        when(blobAdapter.getOutputStream())
                .thenReturn(new ByteArrayOutputStream());
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
                .matches(StreamResult::succeeded, "is succeeded");
    }

    @ParameterizedTest
    @MethodSource("failureStates")
    void transfer_failure(List<String> states) {
        // Arrange
        when(run.status()).thenReturn(states.get(0), states.stream().skip(1).toArray(String[]::new));

        // Act & Assert
        assertThatTransferResult()
                .matches(StreamResult::failed);
    }

    @Test
    void transfer_failed_to_write_complete_blob_failure() {
        // Arrange
        when(run.status()).thenReturn("Succeeded");
        when(blobAdapter.getOutputStream()).thenReturn(new UncloseableOutputStream(new ByteArrayOutputStream()));

        // Act & Assert
        assertThatTransferResult()
                .matches(StreamResult::failed);
        assertThatTransferResult()
                .satisfies(r -> assertThat(r.getFailure().getReason()).isEqualTo(StreamFailure.Reason.GENERAL_ERROR));
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

    ObjectAssert<StreamResult<Void>> assertThatTransferResult() {
        return assertThat(transferManager.transfer(request))
                .succeedsWithin(Duration.ofMinutes(1));
    }
}
