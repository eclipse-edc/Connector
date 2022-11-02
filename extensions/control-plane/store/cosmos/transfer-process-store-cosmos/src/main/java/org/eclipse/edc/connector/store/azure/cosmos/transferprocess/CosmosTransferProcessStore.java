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

package org.eclipse.edc.connector.store.azure.cosmos.transferprocess;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.BadRequestException;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.implementation.RequestRateTooLargeException;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.fasterxml.jackson.core.type.TypeReference;
import dev.failsafe.FailsafeExecutor;
import dev.failsafe.Fallback;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApi;
import org.eclipse.edc.azure.cosmos.CosmosDocument;
import org.eclipse.edc.azure.cosmos.dialect.SqlStatement;
import org.eclipse.edc.azure.cosmos.util.CosmosLeaseContext;
import org.eclipse.edc.connector.store.azure.cosmos.transferprocess.model.TransferProcessDocument;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.failsafe.Failsafe.with;

public class CosmosTransferProcessStore implements TransferProcessStore {


    private static final String NEXT_FOR_STATE_S_PROC_NAME = "nextForState";
    private final CosmosDbApi cosmosDbApi;
    private final CosmosQueryRequestOptions tracingOptions;
    private final TypeManager typeManager;
    private final String partitionKey;
    private final String leaseHolderName;
    private final RetryPolicy<Object> generalRetry;
    private final RetryPolicy<Object> rateLimitRetry;
    private final FailsafeExecutor<Object> failsafeExecutor;
    private final CosmosLeaseContext leaseContext;

    private final Clock clock;

    /**
     * Creates a new instance of the CosmosDB-based transfer process store.
     *
     * @param cosmosDbApi  Api for interaction with CosmosDB.
     * @param typeManager  The {@link TypeManager} that's used for serialization and deserialization
     * @param partitionKey A Partition Key that CosmosDB uses for r/w distribution. Contrary to what CosmosDB suggests, this
     *                     key should be the same for all local (=clustered) connectors, otherwise queries in stored procedures might
     *                     produce incomplete results.
     * @param leaseHolder  A name for the connector that must be unique in the local storage context. That means that all connectors e.g.
     *                     in a local K8s cluster must have unique names. The connectorId is used to lock transfer processes so that no
     * @param retryPolicy  A general retry policy for the CosmosAPI
     */
    public CosmosTransferProcessStore(CosmosDbApi cosmosDbApi, TypeManager typeManager, String partitionKey, String leaseHolder, RetryPolicy<Object> retryPolicy, Clock clock) {

        this.cosmosDbApi = cosmosDbApi;
        this.typeManager = typeManager;
        this.partitionKey = partitionKey;
        leaseHolderName = leaseHolder;
        tracingOptions = new CosmosQueryRequestOptions();
        tracingOptions.setQueryMetricsEnabled(true);
        rateLimitRetry = RetryPolicy.builder()
                .handle(RequestRateTooLargeException.class)
                .withMaxRetries(1).withDelay(Duration.ofSeconds(5))
                .build();

        generalRetry = retryPolicy;

        failsafeExecutor = with(rateLimitRetry, generalRetry);
        leaseContext = CosmosLeaseContext.with(cosmosDbApi, partitionKey, leaseHolder).usingRetry(List.of(rateLimitRetry, generalRetry));

        this.clock = clock;
    }

    @Override
    public TransferProcess find(String id) {
        // we need to read the TransferProcessDocument as Object, because no custom JSON deserialization can be registered
        // with the CosmosDB SDK, so it would not know about subtypes, etc.
        Object obj = findByIdInternal(id);
        return obj != null ? convertToDocument(obj).getWrappedInstance() : null;
    }

    @Override
    public @Nullable
    String processIdForDataRequestId(String transferId) {
        var query = "SELECT * FROM t WHERE t.wrappedInstance.dataRequest.id = '" + transferId + "'";
        var response = failsafeExecutor.get(() -> cosmosDbApi.queryItems(query));
        return response
                .map(this::convertToDocument)
                .map(pd -> pd.getWrappedInstance().getId())
                .findFirst()
                .orElse(null);
    }

    @Override
    public void create(TransferProcess process) {
        Objects.requireNonNull(process.getId(), "TransferProcesses must have an ID!");

        //todo: configure indexing
        var document = new TransferProcessDocument(process, partitionKey);
        failsafeExecutor.run(() -> cosmosDbApi.saveItem(document));
    }

    @Override
    public void update(TransferProcess process) {
        var document = new TransferProcessDocument(process, partitionKey);
        try {
            leaseContext.acquireLease(process.getId());
            failsafeExecutor.run(() -> cosmosDbApi.saveItem(document));
            leaseContext.breakLease(process.getId());
        } catch (BadRequestException ex) {
            throw new EdcException(ex);
        }
    }

    @Override
    public void delete(String processId) {
        try {

            var item = findByIdInternal(processId);
            if (item == null) {
                return;
            }

            var document = convertToDocument(item);

            var lease = document.getLease();
            if (lease != null && !lease.isExpired(clock.millis())) {
                throw new IllegalStateException(String.format("The TransferProcess [%s] cannot be deleted: it is currently leased", processId));
            }
            leaseContext.acquireLease(processId);
            failsafeExecutor.run(() -> cosmosDbApi.deleteItem(processId));
            leaseContext.breakLease(processId);
        } catch (NotFoundException ex) {
            //do nothing
        } catch (CosmosException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public Stream<TransferProcess> findAll(QuerySpec querySpec) {
        var statement = new SqlStatement<>(TransferProcessDocument.class);
        var query = statement.where(querySpec.getFilterExpression())
                .offset(querySpec.getOffset())
                .limit(querySpec.getLimit())
                .orderBy(querySpec.getSortField(), querySpec.getSortOrder() == SortOrder.ASC)
                .getQueryAsSqlQuerySpec();

        var objects = failsafeExecutor.get(() -> cosmosDbApi.queryItems(query));
        return objects.map(this::convertToDocument).map(TransferProcessDocument::getWrappedInstance);
    }

    @Override
    public @NotNull List<TransferProcess> nextForState(int state, int max) {
        tracingOptions.setMaxBufferedItemCount(max);

        var rawJson = with(Fallback.of((String) null), rateLimitRetry, generalRetry)
                .get(() -> cosmosDbApi.invokeStoredProcedure(NEXT_FOR_STATE_S_PROC_NAME, partitionKey, state, max, leaseHolderName));

        if (rawJson == null) {
            return Collections.emptyList();
        }

        //now we need to convert to a list, convert each element in that list to json, and convert that back to a TransferProcessDocument
        var typeRef = new TypeReference<List<Object>>() {
        };
        var l = typeManager.readValue(rawJson, typeRef);

        return l.stream()
                .map(this::convertToDocument)
                .map(CosmosDocument::getWrappedInstance)
                .collect(Collectors.toList());

    }

    private Object findByIdInternal(String processId) {
        return failsafeExecutor.get(() -> cosmosDbApi.queryItemById(processId));
    }

    private TransferProcessDocument convertToDocument(Object databaseDocument) {
        return typeManager.readValue(typeManager.writeValueAsString(databaseDocument), TransferProcessDocument.class);
    }
}
