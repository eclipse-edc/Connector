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

package org.eclipse.edc.connector.dataplane.store.cosmos.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.edc.azure.cosmos.CosmosDocument;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@JsonTypeName("dataspaceconnector:dataflowrequestdocument")
public class DataFlowRequestDocument extends CosmosDocument<Map<String, Object>> {

    private final String id;
    private final long createdAt;
    private final long updatedAt;

    private final DataPlaneStore.State state;

    public DataFlowRequestDocument(String processId,
                                   DataPlaneStore.State state,
                                   long createdAt,
                                   long updatedAt,
                                   String partitionKey) {
        this(buildProperties(processId, state), partitionKey, createdAt, updatedAt);
    }

    @JsonCreator
    public DataFlowRequestDocument(@JsonProperty("wrappedInstance") Map<String, Object> wrappedInstance,
                                   @JsonProperty("partitionKey") String partitionKey,
                                   @JsonProperty("createdAt") long createdAt, @JsonProperty("updatedAt") long updatedAt) {
        super(wrappedInstance, partitionKey);
        id = wrappedInstance.get("processId").toString();
        Integer stateCode = (Integer) wrappedInstance.get("state");
        state = DataPlaneStore.State.from(stateCode);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @NotNull
    private static Map<String, Object> buildProperties(String processId, DataPlaneStore.State state) {
        return Map.of("processId", processId, "state", state.getCode());
    }

    @Override
    public String getId() {
        return id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }


    public DataPlaneStore.State getState() {
        return state;
    }
}
