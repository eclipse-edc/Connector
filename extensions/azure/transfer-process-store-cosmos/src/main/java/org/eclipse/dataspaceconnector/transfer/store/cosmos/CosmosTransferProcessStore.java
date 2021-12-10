/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.store.cosmos;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.BadRequestException;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.implementation.RequestRateTooLargeException;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import net.jodah.failsafe.FailsafeExecutor;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.store.cosmos.model.TransferProcessDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static net.jodah.failsafe.Failsafe.with;

public class CosmosTransferProcessStore implements TransferProcessStore {


    private static final String NEXT_FOR_STATE_S_PROC_NAME = "nextForState";
    private static final String LEASE_S_PROC_NAME = "lease";
    private final CosmosDbApi cosmosDbApi;
    private final CosmosQueryRequestOptions tracingOptions;
    private final TypeManager typeManager;
    private final String partitionKey;
    private final String connectorId;
    private final RetryPolicy<Object> generalRetry;
    private final RetryPolicy<Object> rateLimitRetry;
    private final FailsafeExecutor<Object> failsafeExecutor;

    /**
     * Creates a new instance of the CosmosDB-based transfer process store.
     *
     * @param cosmosDbApi  Api for interaction with CosmosDB.
     * @param typeManager  The {@link TypeManager} that's used for serialization and deserialization
     * @param partitionKey A Partition Key that CosmosDB uses for r/w distribution. Contrary to what CosmosDB suggests, this
     *                     key should be the same for all local (=clustered) connectors, otherwise queries in stored procedures might
     *                     produce incomplete results.
     * @param connectorId  A name for the connector that must be unique in the local storage context. That means that all connectors e.g.
     *                     in a local K8s cluster must have unique names. The connectorId is used to lock transfer processes so that no
     * @param retryPolicy  A general retry policy for the CosmosAPI
     */
    public CosmosTransferProcessStore(CosmosDbApi cosmosDbApi, TypeManager typeManager, String partitionKey, String connectorId, RetryPolicy<Object> retryPolicy) {

        this.cosmosDbApi = cosmosDbApi;
        this.typeManager = typeManager;
        this.partitionKey = partitionKey;
        this.connectorId = connectorId;
        tracingOptions = new CosmosQueryRequestOptions();
        tracingOptions.setQueryMetricsEnabled(true);
        rateLimitRetry = new RetryPolicy<>()
                .handle(RequestRateTooLargeException.class)
                .withMaxRetries(1).withDelay(Duration.ofSeconds(5));

        generalRetry = retryPolicy;

        failsafeExecutor = with(rateLimitRetry, generalRetry);
    }

    @Override
    public TransferProcess find(String id) {
        // we need to read the TransferProcessDocument as Object, because no custom JSON deserialization can be registered
        // with the CosmosDB SDK, so it would not know about subtypes, etc.
        Object obj = failsafeExecutor.get(() -> cosmosDbApi.queryItemById(id, partitionKey));
        return obj != null ? convertObject(obj).getWrappedInstance() : null;
    }

    @Override
    public @Nullable String processIdForTransferId(String transferId) {
        var query = "SELECT * FROM t WHERE t.wrappedInstance.dataRequest.id = '" + transferId + "'";
        var response = failsafeExecutor.get(() -> cosmosDbApi.queryItems(query));
        return response
                .map(this::convertObject)
                .map(pd -> pd.getWrappedInstance().getId())
                .findFirst()
                .orElse(null);
    }

    @Override
    public @NotNull List<TransferProcess> nextForState(int state, int max) {
        tracingOptions.setMaxBufferedItemCount(max);

        var rawJson = with(Fallback.of((String) null), rateLimitRetry, generalRetry)
                .get(() -> cosmosDbApi.invokeStoredProcedure(NEXT_FOR_STATE_S_PROC_NAME, partitionKey, state, max, connectorId));

        if (rawJson == null) {
            return Collections.emptyList();
        }

        //now we need to convert to a list, convert each element in that list to json, and convert that back to a TransferProcessDocument
        var l = typeManager.readValue(rawJson, List.class);

        return (List<TransferProcess>) l.stream().map(typeManager::writeValueAsString)
                .map(json -> typeManager.readValue(json.toString(), TransferProcessDocument.class))
                .map(tp -> ((TransferProcessDocument) tp).getWrappedInstance())
                .collect(Collectors.toList());

    }


    @Override
    public void create(TransferProcess process) {
        Objects.requireNonNull(process.getId(), "TransferProcesses must have an ID!");
        process.transitionInitial();

        //todo: configure indexing
        var document = new TransferProcessDocument(process, partitionKey);
        failsafeExecutor.run(() -> cosmosDbApi.saveItem(document));
    }

    @Override
    public void update(TransferProcess process) {
        var document = new TransferProcessDocument(process, partitionKey);
        try {
            lease(process.getId(), connectorId);
            failsafeExecutor.run(() -> cosmosDbApi.saveItem(document));
            release(process.getId(), connectorId);
        } catch (BadRequestException ex) {
            throw new EdcException(ex);
        }
    }

    @Override
    public void delete(String processId) {
        try {
            lease(processId, connectorId);
            failsafeExecutor.run(() -> cosmosDbApi.deleteItem(processId));
            release(processId, connectorId);
        } catch (NotFoundException ex) {
            //do nothing
        } catch (CosmosException ex) {
            throw new EdcException(ex);
        }
    }

    @Override
    public void createData(String processId, String key, Object data) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updateData(String processId, String key, Object data) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deleteData(String processId, String key) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deleteData(String processId, Set<String> keys) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public <T> T findData(Class<T> type, String processId, String resourceDefinitionId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private TransferProcessDocument convertObject(Object databaseDocument) {
        return typeManager.readValue(typeManager.writeValueAsString(databaseDocument), TransferProcessDocument.class);
    }

    private void release(String processId, Object connectorId) {
        writeLease(processId, connectorId, false);
    }

    private void lease(String processId, String connectorId) {
        writeLease(processId, connectorId, true);
    }

    private void writeLease(String processId, Object connectorId, boolean writeLease) {
        failsafeExecutor.run(() -> cosmosDbApi.invokeStoredProcedure(LEASE_S_PROC_NAME, partitionKey, processId, connectorId, writeLease));
    }
}