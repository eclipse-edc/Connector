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

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.CosmosStoredProcedure;
import com.azure.cosmos.implementation.GoneException;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.implementation.RequestRateTooLargeException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.CosmosStoredProcedureRequestOptions;
import com.azure.cosmos.models.CosmosStoredProcedureResponse;
import com.azure.cosmos.models.PartitionKey;
import net.jodah.failsafe.FailsafeExecutor;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.transfer.store.cosmos.model.TransferProcessDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static net.jodah.failsafe.Failsafe.with;

public class CosmosTransferProcessStore implements TransferProcessStore {


    private static final String NEXT_FOR_STATE_S_PROC_NAME = "nextForState";
    private static final String LEASE_S_PROC_NAME = "lease";
    private final CosmosContainer container;
    private final CosmosQueryRequestOptions tracingOptions;
    private final TypeManager typeManager;
    private final String partitionKey;
    private final String connectorId;
    private final RetryPolicy<Object> generalRetry;
    private final RetryPolicy<Object> rateLimitRetry;
    private FailsafeExecutor<Object> failsafeExecutor;

    /**
     * Creates a new instance of the CosmosDB-based transfer process store.
     *
     * @param container    The CosmosDB-container that'll hold the transfer processes.
     * @param typeManager  The {@link TypeManager} that's used for serialization and deserialization
     * @param partitionKey A Partition Key that CosmosDB uses for r/w distribution. Contrary to what CosmosDB suggests, this
     *                     key should be the same for all local (=clustered) connectors, otherwise queries in stored procedures might
     *                     produce incomplete results.
     * @param connectorId  A name for the connector that must be unique in the local storage context. That means that all connectors e.g.
     *                     in a local K8s cluster must have unique names. The connectorId is used to lock transfer processes so that no
     * @param retryPolicy  A general retry policy for the CosmosAPI
     */
    public CosmosTransferProcessStore(CosmosContainer container, TypeManager typeManager, String partitionKey, String connectorId, RetryPolicy<Object> retryPolicy) {

        this.container = container;
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
        CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        try {
            // we need to read the TransferProcessDocument as Object, because no custom JSON deserialization can be registered
            // with the CosmosDB SDK, so it would not know about subtypes, etc.
            CosmosItemResponse<Object> response = failsafeExecutor.get(() -> container.readItem(id, new PartitionKey(partitionKey), options, Object.class));
            var obj = response.getItem();

            return convertObject(obj).getWrappedInstance();
        } catch (NotFoundException | GoneException ex) {
            return null;
        }

    }

    @Override
    public @Nullable String processIdForTransferId(String transferId) {
        var query = "SELECT * FROM TransferProcessDocument WHERE TransferProcessDocument.dataRequest.id = '" + transferId + "'";

        try {
            var response = failsafeExecutor.get(() -> container.queryItems(query, tracingOptions, Object.class));
            return response.stream()
                    .map(this::convertObject)
                    .map(pd -> pd.getWrappedInstance().getId()).findFirst().orElse(null);
        } catch (CosmosException ex) {
            throw new EdcException(ex);
        }
    }

    @Override
    public @NotNull List<TransferProcess> nextForState(int state, int max) {

        tracingOptions.setMaxBufferedItemCount(max);

        var sproc = getStoredProcedure(NEXT_FOR_STATE_S_PROC_NAME);
        List<Object> params = Arrays.asList(state, max, getConnectorId());
        var options = new CosmosStoredProcedureRequestOptions();
        options.setPartitionKey(new PartitionKey(partitionKey));

        var rawJson = with(Fallback.of((String) null), rateLimitRetry, generalRetry).get(() -> {
            CosmosStoredProcedureResponse response = sproc.execute(params, options);
            return response.getResponseAsString();
        });

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

        CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        //todo: configure indexing
        var document = TransferProcessDocument.from(process, partitionKey);
        try {
            var response = failsafeExecutor.get(() -> container.createItem(document, new PartitionKey(partitionKey), options));
            handleResponse(response);
        } catch (CosmosException cme) {
            throw new EdcException(cme);
        }
    }

    @Override
    public void update(TransferProcess process) {
        var document = TransferProcessDocument.from(process, partitionKey);
        try {
            lease(process.getId(), getConnectorId());
            var response = failsafeExecutor.get(() -> container.upsertItem(document, new PartitionKey(partitionKey), new CosmosItemRequestOptions()));
            handleResponse(response);
            release(process.getId(), getConnectorId());
        } catch (CosmosException cme) {
            throw new EdcException(cme);
        }
    }

    @Override
    public void delete(String processId) {
        try {
            lease(processId, getConnectorId());
            failsafeExecutor = failsafeExecutor;
            var response = failsafeExecutor.get(() -> container.deleteItem(processId, new PartitionKey(partitionKey), new CosmosItemRequestOptions()));
            handleResponse(response);
            release(processId, getConnectorId());
        } catch (NotFoundException ignored) {
            //noop
        } catch (CosmosException cme) {
            throw new EdcException(cme);
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

    private void handleResponse(CosmosItemResponse<?> response) {
        int code = response.getStatusCode();
        if (code < 200 || code >= 300) {
            throw new EdcException("Error during CosmosDB interaction: " + code);
        }
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
        var sproc = getStoredProcedure(LEASE_S_PROC_NAME);
        List<Object> args = Arrays.asList(processId, connectorId, writeLease);
        var options = new CosmosStoredProcedureRequestOptions();
        options.setPartitionKey(new PartitionKey(partitionKey));

        var code = failsafeExecutor.get(() -> {
            var response = sproc.execute(args, options);
            return response.getStatusCode();
        });

        if (code < 200 || code >= 300) {
            throw new EdcException("Error breaking lease on process '" + processId + "': " + code);
        }
    }


    private String getConnectorId() {
        return connectorId;
    }

    private CosmosStoredProcedure getStoredProcedure(String sprocName) {
        return container.getScripts().getStoredProcedure(sprocName);
    }

}
