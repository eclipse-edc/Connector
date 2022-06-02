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

import com.azure.core.credential.AzureSasCredential;
import com.azure.resourcemanager.datafactory.models.PipelineResource;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureSasToken;
import org.eclipse.dataspaceconnector.azure.blob.core.api.BlobStoreApi;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.ERROR_RETRY;

/**
 * Service for performing data transfers in Azure Data Factory.
 */
public class AzureDataFactoryTransferManager {
    // Name of the empty blob used to indicate completion. Used by consumer-side status checker.
    private static final String COMPLETE_BLOB_NAME = ".complete";
    private final Monitor monitor;
    private final Duration maxDuration;
    private final Clock clock;
    private final DataFactoryClient client;
    private final DataFactoryPipelineFactory pipelineFactory;
    private final BlobStoreApi blobStoreApi;
    private final TypeManager typeManager;
    private final KeyVaultClient keyVaultClient;
    private final Duration pollDelay;

    public AzureDataFactoryTransferManager(
            Monitor monitor,
            DataFactoryClient client,
            DataFactoryPipelineFactory pipelineFactory,
            Duration maxDuration,
            Clock clock,
            BlobStoreApi blobStoreApi,
            TypeManager typeManager,
            KeyVaultClient keyVaultClient,
            Duration pollDelay
    ) {
        this.monitor = monitor;
        this.client = client;
        this.pipelineFactory = pipelineFactory;
        this.maxDuration = maxDuration;
        this.clock = clock;
        this.blobStoreApi = blobStoreApi;
        this.typeManager = typeManager;
        this.keyVaultClient = keyVaultClient;
        this.pollDelay = pollDelay;
    }

    /**
     * Transfers data from source to destination.
     *
     * @param request the data flow request.
     * @return a {@link CompletableFuture} that completes when the data transfer completes.
     */
    public CompletableFuture<StatusResult<Void>> transfer(DataFlowRequest request) {

        PipelineResource pipeline = pipelineFactory.createPipeline(request);

        // Destination
        var dataAddress = request.getDestinationDataAddress();
        var secret = keyVaultClient.getSecret(dataAddress.getKeyName());
        var token = typeManager.readValue(secret.getValue(), AzureSasToken.class);
        var accountName = dataAddress.getProperty(AzureBlobStoreSchema.ACCOUNT_NAME);
        var containerName = dataAddress.getProperty(AzureBlobStoreSchema.CONTAINER_NAME);

        var runId = client.runPipeline(pipeline).runId();

        monitor.info("Created ADF pipeline for " + request.getProcessId() + ". Run id is " + runId);

        return awaitRunCompletion(runId)
                .thenApply(result -> {
                    if (result.succeeded()) {
                        return complete(accountName, containerName, token.getSas());
                    }
                    return result;
                })
                .exceptionally(throwable -> {
                    var error = "Unhandled exception raised when transferring data";
                    monitor.severe(error, throwable);
                    return StatusResult.failure(ERROR_RETRY, error + ":" + throwable.getMessage());
                });
    }

    @NotNull
    private CompletableFuture<StatusResult<Void>> awaitRunCompletion(String runId) {
        monitor.info("Awaiting ADF pipeline completion for run " + runId);

        var timeout = clock.instant().plus(maxDuration);
        while (clock.instant().isBefore(timeout)) {
            var pipelineRun = client.getPipelineRun(runId);
            var runStatusValue = pipelineRun.status();
            var message = pipelineRun.message();
            monitor.info("ADF run status is " + runStatusValue + " with message [" + message + "] for run " + runId);
            DataFactoryPipelineRunStates runStatus;

            try {
                runStatus = DataFactoryPipelineRunStates.valueOf(runStatusValue);
            } catch (IllegalArgumentException e) {
                return completedFuture(StatusResult.failure(ERROR_RETRY,
                        format("ADF run in unexpected state %s with message: %s", runStatusValue, message)));
            }
            if (runStatus.succeeded) {
                return completedFuture(StatusResult.success());
            }
            if (runStatus.failed) {
                return completedFuture(StatusResult.failure(ERROR_RETRY,
                        format("ADF run in state %s with message: %s", runStatusValue, message)));
            }

            try {
                TimeUnit.MILLISECONDS.sleep(pollDelay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        client.cancelPipelineRun(runId);
        return completedFuture(StatusResult.failure(ERROR_RETRY, "ADF run timed out"));
    }

    private StatusResult<Void> complete(String accountName, String containerName, String sharedAccessSignature) {
        try {
            // Write an empty blob to indicate completion
            blobStoreApi.getBlobAdapter(accountName, containerName, COMPLETE_BLOB_NAME, new AzureSasCredential(sharedAccessSignature))
                    .getOutputStream().close();
            return StatusResult.success();
        } catch (IOException e) {
            return StatusResult.failure(ERROR_RETRY, format("Error creating blob %s on account %s", COMPLETE_BLOB_NAME, accountName));
        }
    }

    /**
     * States of a pipeline run, as returned from the Data Factory API.
     *
     * @see <a href="https://docs.microsoft.com/rest/api/datafactory/pipeline-runs/get#pipelinerun">PipelineRun</a>
     */
    @SuppressWarnings("unused")
    private enum DataFactoryPipelineRunStates {
        Queued(false, false),
        InProgress(false, false),
        Succeeded(true, false),
        Failed(false, true),
        Canceling(false, true),
        Cancelled(false, true);

        final boolean succeeded;
        final boolean failed;

        DataFactoryPipelineRunStates(boolean succeeded, boolean failed) {
            this.succeeded = succeeded;
            this.failed = failed;
        }
    }
}
