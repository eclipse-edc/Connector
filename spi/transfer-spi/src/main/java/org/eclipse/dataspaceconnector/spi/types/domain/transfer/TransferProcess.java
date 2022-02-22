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

package org.eclipse.dataspaceconnector.spi.types.domain.transfer;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.telemetry.TraceCarrier;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

/**
 * Represents a data transfer process.
 * <br/>
 * A data transfer process exists on both the consumer and provider connector; it is a representation of the data sharing transaction from the perspective of each endpoint. The data
 * transfer process is modeled as a "loosely" coordinated state machine on each connector. The state transitions are symmetric on the consumer and provider with the exception that
 * the consumer process has two additional states for request/request ack.
 * <br/>
 * The consumer transitions are:
 *
 * <pre>
 * {@link TransferProcessStates#INITIAL} ->
 * {@link TransferProcessStates#PROVISIONING} ->
 * {@link TransferProcessStates#PROVISIONED} ->
 * {@link TransferProcessStates#REQUESTED} ->
 * {@link TransferProcessStates#REQUESTED_ACK} ->
 * {@link TransferProcessStates#IN_PROGRESS} | {@link TransferProcessStates#STREAMING} ->
 * {@link TransferProcessStates#COMPLETED} ->
 * {@link TransferProcessStates#DEPROVISIONING} ->
 * {@link TransferProcessStates#DEPROVISIONED} ->
 * {@link TransferProcessStates#ENDED} ->
 * {@link TransferProcessStates#CANCELLED} -> optional, reachable from every state except ENDED, COMPLETED or ERROR
 * </pre>
 * <br/>
 * <br/>
 * The provider transitions are:
 *
 * <pre>
 * {@link TransferProcessStates#INITIAL} ->
 * {@link TransferProcessStates#PROVISIONING} ->
 * {@link TransferProcessStates#PROVISIONED} ->
 * {@link TransferProcessStates#IN_PROGRESS} | {@link TransferProcessStates#STREAMING} ->
 * {@link TransferProcessStates#COMPLETED} ->
 * {@link TransferProcessStates#DEPROVISIONING} ->
 * {@link TransferProcessStates#DEPROVISIONED} ->
 * {@link TransferProcessStates#ENDED} ->
 * {@link TransferProcessStates#CANCELLED} -> optional, reachable from every state except ENDED, COMPLETED or ERROR
 * </pre>
 * <br/>
 */
@JsonTypeName("dataspaceconnector:transferprocess")
@JsonDeserialize(builder = TransferProcess.Builder.class)
public class TransferProcess implements TraceCarrier {

    private String id;
    private Type type = Type.CONSUMER;
    private int state;
    private int stateCount = TransferProcessStates.UNSAVED.code();
    private long stateTimestamp;
    private Map<String, String> traceContext = new HashMap<>();
    private String errorDetail;
    private DataRequest dataRequest;
    private ResourceManifest resourceManifest;
    private ProvisionedResourceSet provisionedResourceSet;

    private TransferProcess() {
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public int getState() {
        return state;
    }

    public int getStateCount() {
        return stateCount;
    }

    public long getStateTimestamp() {
        return stateTimestamp;
    }

    public Map<String, String> getTraceContext() {
        return Collections.unmodifiableMap(traceContext);
    }

    public DataRequest getDataRequest() {
        return dataRequest;
    }

    public ResourceManifest getResourceManifest() {
        return resourceManifest;
    }

    public ProvisionedResourceSet getProvisionedResourceSet() {
        return provisionedResourceSet;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void transitionInitial() {
        transition(TransferProcessStates.INITIAL, TransferProcessStates.UNSAVED);
    }

    public void transitionProvisioning(ResourceManifest manifest) {
        transition(TransferProcessStates.PROVISIONING, TransferProcessStates.INITIAL, TransferProcessStates.PROVISIONING);
        resourceManifest = manifest;
        resourceManifest.setTransferProcessId(id);
    }

    public void addProvisionedResource(ProvisionedResource resource) {
        if (provisionedResourceSet == null) {
            provisionedResourceSet = ProvisionedResourceSet.Builder.newInstance().transferProcessId(id).build();
        }
        provisionedResourceSet.addResource(resource);
    }

    public boolean provisioningComplete() {
        if (resourceManifest == null) {
            return false;
        }

        Set<String> definitions = resourceManifest.getDefinitions().stream()
                .map(ResourceDefinition::getId)
                .collect(toSet());

        Set<String> resources = Optional.ofNullable(provisionedResourceSet).stream()
                .flatMap(it -> it.getResources().stream())
                .map(ProvisionedResource::getResourceDefinitionId)
                .collect(toSet());

        return definitions.equals(resources);
    }

    public void transitionProvisioned() {
        // requested is allowed to support retries
        transition(TransferProcessStates.PROVISIONED, TransferProcessStates.PROVISIONING, TransferProcessStates.PROVISIONED, TransferProcessStates.REQUESTED);
    }

    public void transitionRequested() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no REQUESTED state");
        }
        transition(TransferProcessStates.REQUESTED, TransferProcessStates.PROVISIONED, TransferProcessStates.REQUESTED);

    }

    public void transitionRequestAck() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no REQUESTED state");
        }
        transition(TransferProcessStates.REQUESTED_ACK, TransferProcessStates.REQUESTED);
    }

    public void transitionInProgressOrStreaming() {
        var dataRequest = getDataRequest();
        if (dataRequest.getTransferType().isFinite()) {
            transitionInProgress();
        } else {
            transitionStreaming();
        }
    }

    public void transitionInProgress() {
        if (type == Type.CONSUMER) {
            // the consumer must first transition to the request/ack states before in progress
            transition(TransferProcessStates.IN_PROGRESS, TransferProcessStates.REQUESTED, TransferProcessStates.REQUESTED_ACK);
        } else {
            // the provider transitions from provisioned to in progress directly
            transition(TransferProcessStates.IN_PROGRESS, TransferProcessStates.REQUESTED, TransferProcessStates.PROVISIONED);
        }
    }

    public void transitionStreaming() {
        if (type == Type.CONSUMER) {
            // the consumer must first transition to the request/ack states before in progress
            transition(TransferProcessStates.STREAMING, TransferProcessStates.REQUESTED_ACK);
        } else {
            // the provider transitions from provisioned to in progress directly
            transition(TransferProcessStates.STREAMING, TransferProcessStates.PROVISIONED);
        }
    }

    public void transitionCompleted() {
        // consumers are in REQUESTED_ACK state after sending a request to the provider, they can directly transition to COMPLETED when the transfer is complete
        transition(TransferProcessStates.COMPLETED, TransferProcessStates.COMPLETED, TransferProcessStates.IN_PROGRESS, TransferProcessStates.REQUESTED_ACK, TransferProcessStates.STREAMING);
    }

    public void transitionDeprovisioning() {
        transition(TransferProcessStates.DEPROVISIONING, TransferProcessStates.COMPLETED, TransferProcessStates.DEPROVISIONING, TransferProcessStates.DEPROVISIONING_REQ);
    }

    public void transitionDeprovisioned() {
        transition(TransferProcessStates.DEPROVISIONED, TransferProcessStates.DEPROVISIONING, TransferProcessStates.DEPROVISIONING_REQ, TransferProcessStates.DEPROVISIONED);
    }

    public void transitionCancelled() {
        // alternatively we could take the ".values()" array, and remove disallowed once, but this
        // seems more explicit
        var allowedStates = new TransferProcessStates[]{
                TransferProcessStates.UNSAVED, TransferProcessStates.INITIAL,
                TransferProcessStates.PROVISIONING, TransferProcessStates.PROVISIONED,
                TransferProcessStates.REQUESTED, TransferProcessStates.REQUESTED_ACK,
                TransferProcessStates.IN_PROGRESS, TransferProcessStates.STREAMING,
                TransferProcessStates.DEPROVISIONED, TransferProcessStates.DEPROVISIONING_REQ,
                TransferProcessStates.DEPROVISIONING, TransferProcessStates.CANCELLED
        };
        transition(TransferProcessStates.CANCELLED, allowedStates);
    }

    /**
     * Indicates that the transfer process is completed and that it should be deprovisioned
     */
    public void transitionDeprovisionRequested() {
        transition(TransferProcessStates.DEPROVISIONING_REQ, TransferProcessStates.COMPLETED, TransferProcessStates.DEPROVISIONING_REQ);
    }


    public void transitionEnded() {
        transition(TransferProcessStates.ENDED, TransferProcessStates.DEPROVISIONED);
    }

    public void transitionError(@Nullable String errorDetail) {
        state = TransferProcessStates.ERROR.code();
        this.errorDetail = errorDetail;
        stateCount = 1;
        updateStateTimestamp();
    }


    public void rollbackState(TransferProcessStates state) {
        this.state = state.code();
        stateCount = 1;
        updateStateTimestamp();
    }

    public TransferProcess copy() {
        return Builder.newInstance().id(id).state(state).stateTimestamp(stateTimestamp).stateCount(stateCount).resourceManifest(resourceManifest).dataRequest(dataRequest)
                .provisionedResourceSet(provisionedResourceSet).traceContext(traceContext).type(type).errorDetail(errorDetail).build();
    }

    public Builder toBuilder() {
        return new Builder(copy());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferProcess that = (TransferProcess) o;
        return id.equals(that.id);
    }

    @Override
    public String toString() {
        return "TransferProcess{" +
                "id='" + id + '\'' +
                ", state=" + TransferProcessStates.from(state) +
                ", stateTimestamp=" + Instant.ofEpochMilli(stateTimestamp) +
                '}';
    }

    public void updateStateTimestamp() {
        stateTimestamp = Instant.now().toEpochMilli();
    }

    private void transition(TransferProcessStates end, TransferProcessStates... starts) {
        if (end.code() < state) {
            return; //we cannot transition "back"
        }

        if (Arrays.stream(starts).noneMatch(s -> s.code() == state)) {
            throw new IllegalStateException(format("Cannot transition from state %s to %s", TransferProcessStates.from(state), TransferProcessStates.from(end.code())));
        }
        stateCount = state == end.code() ? stateCount + 1 : 1;
        state = end.code();
        updateStateTimestamp();
    }

    public enum Type {
        CONSUMER, PROVIDER
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private final TransferProcess process;

        private Builder(TransferProcess process) {
            this.process = process;
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new TransferProcess());
        }

        public Builder id(String id) {
            process.id = id;
            return this;
        }

        public Builder type(Type type) {
            process.type = type;
            return this;
        }

        public Builder state(int value) {
            process.state = value;
            return this;
        }

        public Builder stateCount(int value) {
            process.stateCount = value;
            return this;
        }

        public Builder stateTimestamp(long value) {
            process.stateTimestamp = value;
            return this;
        }

        public Builder dataRequest(DataRequest request) {
            process.dataRequest = request;
            return this;
        }

        public Builder resourceManifest(ResourceManifest manifest) {
            process.resourceManifest = manifest;
            return this;
        }

        public Builder provisionedResourceSet(ProvisionedResourceSet set) {
            process.provisionedResourceSet = set;
            return this;
        }

        public Builder errorDetail(String errorDetail) {
            process.errorDetail = errorDetail;
            return this;
        }

        public Builder traceContext(Map<String, String> traceContext) {
            process.traceContext = traceContext;
            return this;
        }

        public TransferProcess build() {
            Objects.requireNonNull(process.id, "id");
            if (process.state == TransferProcessStates.UNSAVED.code() && process.stateTimestamp == 0) {
                process.stateTimestamp = Instant.now().toEpochMilli();
            }
            if (process.resourceManifest != null) {
                process.resourceManifest.setTransferProcessId(process.id);
            }

            if (process.provisionedResourceSet != null) {
                process.provisionedResourceSet.setTransferProcessId(process.id);
            }

            if (process.dataRequest != null) {
                process.dataRequest.setProcessId(process.id);
            }
            return process;
        }

    }
}
