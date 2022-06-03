/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.spi.types.domain.transfer;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.telemetry.TraceCarrier;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.CANCELLED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.ENDED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.ERROR;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.INITIAL;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.IN_PROGRESS;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONING;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.REQUESTED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.REQUESTING;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.STREAMING;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.UNSAVED;

/**
 * Represents a data transfer process.
 * <p>
 * A data transfer process exists on both the consumer and provider connector; it is a representation of the data sharing transaction from the perspective of each endpoint. The data
 * transfer process is modeled as a "loosely" coordinated state machine on each connector. The state transitions are symmetric on the consumer and provider with the exception that
 * the consumer process has two additional states for request/request ack.
 * <p>
 * The consumer transitions are:
 *
 * <pre>
 * {@link TransferProcessStates#INITIAL} -&gt;
 * {@link TransferProcessStates#PROVISIONING} -&gt;
 * {@link TransferProcessStates#PROVISIONED} -&gt;
 * {@link TransferProcessStates#REQUESTING} -&gt;
 * {@link TransferProcessStates#REQUESTED} -&gt;
 * {@link TransferProcessStates#IN_PROGRESS} | {@link TransferProcessStates#STREAMING} -&gt;
 * {@link TransferProcessStates#COMPLETED} -&gt;
 * {@link TransferProcessStates#DEPROVISIONING} -&gt;
 * {@link TransferProcessStates#DEPROVISIONED} -&gt;
 * {@link TransferProcessStates#ENDED} -&gt;
 * {@link TransferProcessStates#CANCELLED} -&gt; optional, reachable from every state except ENDED, COMPLETED or ERROR
 * </pre>
 * <p>
 * <p>
 * The provider transitions are:
 *
 * <pre>
 * {@link TransferProcessStates#INITIAL} -&gt;
 * {@link TransferProcessStates#PROVISIONING} -&gt;
 * {@link TransferProcessStates#PROVISIONED} -&gt;
 * {@link TransferProcessStates#IN_PROGRESS} | {@link TransferProcessStates#STREAMING} -&gt;
 * {@link TransferProcessStates#COMPLETED} -&gt;
 * {@link TransferProcessStates#DEPROVISIONING} -&gt;
 * {@link TransferProcessStates#DEPROVISIONED} -&gt;
 * {@link TransferProcessStates#ENDED} -&gt;
 * {@link TransferProcessStates#CANCELLED} -&gt; optional, reachable from every state except ENDED, COMPLETED or ERROR
 * </pre>
 * <p>
 */
@JsonTypeName("dataspaceconnector:transferprocess")
@JsonDeserialize(builder = TransferProcess.Builder.class)
public class TransferProcess implements TraceCarrier {

    private String id;
    private Type type = Type.CONSUMER;
    private long createdTimestamp;
    private int state;
    private int stateCount = UNSAVED.code();
    private long stateTimestamp;
    private Map<String, String> traceContext = new HashMap<>();
    private String errorDetail;
    private DataRequest dataRequest;
    private DataAddress contentDataAddress;
    private ResourceManifest resourceManifest;
    private ProvisionedResourceSet provisionedResourceSet;
    private List<DeprovisionedResource> deprovisionedResources = new ArrayList<>();
    private Clock clock = Clock.systemUTC();

    private TransferProcess() {
    }

    public String getId() {
        return id;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
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

    @Override
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

    public DataAddress getContentDataAddress() {
        return contentDataAddress;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void transitionInitial() {
        transition(INITIAL, UNSAVED);
    }

    public void transitionProvisioning(ResourceManifest manifest) {
        transition(PROVISIONING, INITIAL, PROVISIONING);
        resourceManifest = manifest;
        resourceManifest.setTransferProcessId(id);
    }

    public void addProvisionedResource(ProvisionedResource resource) {
        if (provisionedResourceSet == null) {
            provisionedResourceSet = ProvisionedResourceSet.Builder.newInstance().transferProcessId(id).build();
        }
        provisionedResourceSet.addResource(resource);
    }

    public void addContentDataAddress(DataAddress dataAddress) {
        contentDataAddress = dataAddress;
    }

    public void addDeprovisionedResource(DeprovisionedResource resource) {
        deprovisionedResources.add(resource);
    }

    @Nullable
    public ProvisionedResource getProvisionedResource(String id) {
        if (provisionedResourceSet == null) {
            return null;
        }
        return provisionedResourceSet.getResources().stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Returns the collection of resources that have not been provisioned.
     */
    @JsonIgnore
    @NotNull
    public List<ResourceDefinition> getResourcesToProvision() {
        if (resourceManifest == null) {
            return emptyList();
        }
        if (provisionedResourceSet == null) {
            return unmodifiableList(resourceManifest.getDefinitions());
        }
        var provisionedResources = provisionedResourceSet.getResources().stream().map(ProvisionedResource::getResourceDefinitionId).collect(toSet());
        return resourceManifest.getDefinitions().stream().filter(r -> !provisionedResources.contains(r.getId())).collect(toList());
    }

    public boolean provisioningComplete() {
        if (resourceManifest == null) {
            return false;
        }

        return getResourcesToProvision().isEmpty();
    }

    /**
     * Returns the collection of resources that have not been deprovisioned.
     */
    @JsonIgnore
    @NotNull
    public List<ProvisionedResource> getResourcesToDeprovision() {
        if (provisionedResourceSet == null) {
            return emptyList();
        }

        var deprovisionedResources = this.deprovisionedResources.stream().map(DeprovisionedResource::getProvisionedResourceId).collect(toSet());
        return provisionedResourceSet.getResources().stream().filter(r -> !deprovisionedResources.contains(r.getId())).collect(toList());
    }

    public boolean deprovisionComplete() {
        return getResourcesToDeprovision().isEmpty();
    }

    public void transitionProvisioned() {
        // requested is allowed to support retries
        transition(PROVISIONED, PROVISIONING, PROVISIONED, REQUESTED);
    }

    public void transitionRequesting() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no REQUESTING state");
        }
        transition(REQUESTING, PROVISIONED, REQUESTING);
    }

    public void transitionRequested() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no REQUESTED state");
        }
        transition(REQUESTED, PROVISIONED, REQUESTING, REQUESTED);

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
            transition(IN_PROGRESS, REQUESTED, IN_PROGRESS);
        } else {
            // the provider transitions from provisioned to in progress directly
            transition(IN_PROGRESS, REQUESTED, PROVISIONED, IN_PROGRESS);
        }
    }

    public void transitionStreaming() {
        if (type == Type.CONSUMER) {
            // the consumer must first transition to the request/ack states before in progress
            transition(STREAMING, REQUESTED, STREAMING);
        } else {
            // the provider transitions from provisioned to in progress directly
            transition(STREAMING, PROVISIONED, STREAMING);
        }
    }

    public void transitionCompleted() {
        // consumers are in REQUESTED state after sending a request to the provider, they can directly transition to COMPLETED when the transfer is complete
        transition(COMPLETED, COMPLETED, IN_PROGRESS, REQUESTED, STREAMING);
    }

    public void transitionDeprovisioning() {
        transition(DEPROVISIONING, COMPLETED, DEPROVISIONING);
    }

    public void transitionDeprovisioned() {
        transition(DEPROVISIONED, DEPROVISIONING, DEPROVISIONED);
    }

    public void transitionCancelled() {
        // alternatively we could take the ".values()" array, and remove disallowed once, but this
        // seems more explicit
        var allowedStates = new TransferProcessStates[]{
                UNSAVED, INITIAL,
                PROVISIONING, PROVISIONED,
                REQUESTED, REQUESTING,
                IN_PROGRESS, STREAMING,
                DEPROVISIONED, DEPROVISIONING,
                CANCELLED
        };
        transition(CANCELLED, allowedStates);
    }

    public void transitionEnded() {
        transition(ENDED, DEPROVISIONED);
    }

    public void transitionError(@Nullable String errorDetail) {
        this.errorDetail = errorDetail;
        state = ERROR.code();
        stateCount = 1;
        updateStateTimestamp();
    }

    public TransferProcess copy() {
        return Builder.newInstance()
                .id(id)
                .state(state)
                .stateTimestamp(stateTimestamp)
                .stateCount(stateCount)
                .resourceManifest(resourceManifest)
                .dataRequest(dataRequest)
                .provisionedResourceSet(provisionedResourceSet)
                .contentDataAddress(contentDataAddress)
                .traceContext(traceContext)
                .type(type)
                .createdTimestamp(createdTimestamp)
                .errorDetail(errorDetail)
                .clock(clock)
                .build();
    }

    public Builder toBuilder() {
        return new Builder(copy());
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
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TransferProcess{" +
                "id='" + id + '\'' +
                ", state=" + TransferProcessStates.from(state) +
                ", stateTimestamp=" + Instant.ofEpochMilli(stateTimestamp) +
                '}';
    }

    /**
     * Sets the state timestamp to the clock time.
     *
     * @see Builder#clock(Clock)
     */
    public void updateStateTimestamp() {
        stateTimestamp = clock.millis();
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

        public Builder clock(Clock clock) {
            process.clock = clock;
            return this;
        }

        public Builder createdTimestamp(long value) {
            process.createdTimestamp = value;
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

        public Builder contentDataAddress(DataAddress dataAddress) {
            process.contentDataAddress = dataAddress;
            return this;
        }

        public Builder provisionedResourceSet(ProvisionedResourceSet set) {
            process.provisionedResourceSet = set;
            return this;
        }

        public Builder deprovisionedResources(List<DeprovisionedResource> resources) {
            process.deprovisionedResources = resources;
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
            Objects.requireNonNull(process.clock, "clock");
            if (process.state == UNSAVED.code() && process.stateTimestamp == 0) {
                process.stateTimestamp = process.clock.millis();
            }
            if (process.resourceManifest != null) {
                process.resourceManifest.setTransferProcessId(process.id);
            }

            if (process.provisionedResourceSet != null) {
                process.provisionedResourceSet.setTransferProcessId(process.id);
            }

            if (process.dataRequest != null) {
                process.dataRequest.associateWithProcessId(process.id);
            }
            return process;
        }

    }
}
