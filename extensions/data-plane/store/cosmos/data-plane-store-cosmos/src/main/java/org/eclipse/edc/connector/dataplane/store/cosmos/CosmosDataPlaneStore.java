/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.store.cosmos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApi;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.connector.dataplane.store.cosmos.model.DataFlowRequestDocument;
import org.eclipse.edc.spi.EdcException;

import java.time.Clock;
import java.util.Optional;

import static dev.failsafe.Failsafe.with;

public class CosmosDataPlaneStore implements DataPlaneStore {

    private final CosmosDbApi cosmosDbApi;
    private final ObjectMapper objectMapper;
    private final RetryPolicy<Object> retryPolicy;
    private final String partitionKey;


    private final Clock clock;

    public CosmosDataPlaneStore(CosmosDbApi cosmosDbApi, ObjectMapper objectMapper, RetryPolicy<Object> retryPolicy, String partitionKey, Clock clock) {
        this.cosmosDbApi = cosmosDbApi;
        this.objectMapper = objectMapper;
        this.retryPolicy = retryPolicy;
        this.partitionKey = partitionKey;
        this.clock = clock;
    }

    @Override
    public void received(String processId) {
        upsert(processId, State.RECEIVED);
    }

    @Override
    public void completed(String processId) {
        upsert(processId, State.COMPLETED);
    }

    @Override
    public State getState(String processId) {
        return Optional.ofNullable(findByIdInternal(processId)).map(DataFlowRequestDocument::getState).orElse(State.NOT_TRACKED);
    }

    private void upsert(String processId, State state) {
        var request = findByIdInternal(processId);
        var ts = clock.millis();
        if (request == null) {
            save(new DataFlowRequestDocument(processId, state, ts, ts, partitionKey));
        } else {
            save(new DataFlowRequestDocument(request.getId(), state, request.getCreatedAt(), ts, partitionKey));
        }
    }

    private void save(DataFlowRequestDocument doc) {
        with(retryPolicy).run(() -> cosmosDbApi.saveItem(doc));
    }

    private DataFlowRequestDocument findByIdInternal(String processorId) {
        var request = with(retryPolicy).get(() -> cosmosDbApi.queryItemById(processorId));
        return request != null ? convert(request) : null;
    }


    private DataFlowRequestDocument convert(Object object) {
        String json = null;
        try {
            json = objectMapper.writeValueAsString(object);
            return objectMapper.readValue(json, DataFlowRequestDocument.class);

        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
    }
}
