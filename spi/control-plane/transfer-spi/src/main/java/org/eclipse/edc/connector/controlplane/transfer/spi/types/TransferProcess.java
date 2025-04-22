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
 *       Mercedes-Benz Tech Innovation GmbH - connector id removal
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.spi.types;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.entity.ProtocolMessages;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PROVISIONED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PROVISIONING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PROVISIONING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING_REQUESTED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Represents a data transfer process.
 * <p>
 * A data transfer process exists on both the consumer and provider connector; it is a representation of the data
 * sharing transaction from the perspective of each endpoint. The data transfer process is modeled as a "loosely"
 * coordinated state machine on each connector. The state transitions are symmetric on the consumer and provider with
 * the exception that the consumer process has two additional states for request/request ack.
 * <p>
 * The consumer transitions are:
 *
 * <pre>
 * {@link TransferProcessStates#INITIAL} -&gt;
 * {@link TransferProcessStates#PROVISIONING} -&gt;
 * {@link TransferProcessStates#PROVISIONED} -&gt;
 * {@link TransferProcessStates#REQUESTING} -&gt;
 * {@link TransferProcessStates#REQUESTED} -&gt;
 * {@link TransferProcessStates#STARTED} -&gt;
 * {@link TransferProcessStates#COMPLETED} -&gt;
 * {@link TransferProcessStates#DEPROVISIONING} -&gt;
 * {@link TransferProcessStates#DEPROVISIONED} -&gt;
 * {@link TransferProcessStates#TERMINATED} -&gt;
 * </pre>
 * <p>
 * <p>
 * The provider transitions are:
 *
 * <pre>
 * {@link TransferProcessStates#INITIAL} -&gt;
 * {@link TransferProcessStates#PROVISIONING} -&gt;
 * {@link TransferProcessStates#PROVISIONED} -&gt;
 * {@link TransferProcessStates#STARTED} -&gt;
 * {@link TransferProcessStates#COMPLETED} -&gt;
 * {@link TransferProcessStates#DEPROVISIONING} -&gt;
 * {@link TransferProcessStates#DEPROVISIONED} -&gt;
 * {@link TransferProcessStates#TERMINATED} -&gt;
 * </pre>
 * <p>
 */
@JsonTypeName("dataspaceconnector:transferprocess")
@JsonDeserialize(builder = TransferProcess.Builder.class)
public class TransferProcess extends StatefulEntity<TransferProcess> {

    public static final String TRANSFER_PROCESS_TYPE = EDC_NAMESPACE + "TransferProcess";
    public static final String TRANSFER_PROCESS_CREATED_AT = EDC_NAMESPACE + "createdAt";
    public static final String TRANSFER_PROCESS_CORRELATION_ID = EDC_NAMESPACE + "correlationId";
    public static final String TRANSFER_PROCESS_STATE = EDC_NAMESPACE + "state";
    public static final String TRANSFER_PROCESS_STATE_TIMESTAMP = EDC_NAMESPACE + "stateTimestamp";
    public static final String TRANSFER_PROCESS_ASSET_ID = EDC_NAMESPACE + "assetId";
    public static final String TRANSFER_PROCESS_CONTRACT_ID = EDC_NAMESPACE + "contractId";
    public static final String TRANSFER_PROCESS_PRIVATE_PROPERTIES = EDC_NAMESPACE + "privateProperties";
    public static final String TRANSFER_PROCESS_TYPE_TYPE = EDC_NAMESPACE + "type";
    public static final String TRANSFER_PROCESS_TRANSFER_TYPE = EDC_NAMESPACE + "transferType";
    public static final String TRANSFER_PROCESS_ERROR_DETAIL = EDC_NAMESPACE + "errorDetail";
    public static final String TRANSFER_PROCESS_DATA_DESTINATION = EDC_NAMESPACE + "dataDestination";
    public static final String TRANSFER_PROCESS_CALLBACK_ADDRESSES = EDC_NAMESPACE + "callbackAddresses";

    private Type type = CONSUMER;
    private String protocol;
    private String correlationId;
    private String counterPartyAddress;
    private DataAddress dataDestination;
    private String assetId;
    private String contractId;
    private DataAddress contentDataAddress;
    private ResourceManifest resourceManifest;
    private ProvisionedResourceSet provisionedResourceSet = ProvisionedResourceSet.Builder.newInstance().build();
    private List<DeprovisionedResource> deprovisionedResources = new ArrayList<>();
    private Map<String, Object> privateProperties = new HashMap<>();
    private List<CallbackAddress> callbackAddresses = new ArrayList<>();
    private ProtocolMessages protocolMessages = new ProtocolMessages();
    private String transferType;
    private String dataPlaneId;

    private TransferProcess() {
    }

    public List<DeprovisionedResource> getDeprovisionedResources() {
        return deprovisionedResources;
    }

    public Type getType() {
        return type;
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

    public void setContentDataAddress(DataAddress dataAddress) {
        contentDataAddress = dataAddress;
    }

    public void transitionProvisioning(ResourceManifest manifest) {
        transition(PROVISIONING, INITIAL, PROVISIONING);
        resourceManifest = manifest;
        resourceManifest.setTransferProcessId(id);
    }

    public void addProvisionedResource(ProvisionedResource resource) {
        provisionedResourceSet.addResource(resource);
        setModified();

    }

    public void addDeprovisionedResource(DeprovisionedResource resource) {
        deprovisionedResources.add(resource);
        setModified();
    }

    @Nullable
    public ProvisionedResource getProvisionedResource(String id) {
        return getProvisionedResources().stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
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

    public Map<String, Object> getPrivateProperties() {
        return Collections.unmodifiableMap(privateProperties);
    }

    public List<CallbackAddress> getCallbackAddresses() {
        return Collections.unmodifiableList(callbackAddresses);
    }

    public boolean deprovisionComplete() {
        return getResourcesToDeprovision().isEmpty();
    }

    public boolean shouldIgnoreIncomingMessage(@NotNull String messageId) {
        return protocolMessages.isAlreadyReceived(messageId) || TransferProcessStates.isFinal(state);
    }

    public ProtocolMessages getProtocolMessages() {
        return protocolMessages;
    }

    public String lastSentProtocolMessage() {
        return protocolMessages.getLastSent();
    }

    public void lastSentProtocolMessage(String id) {
        protocolMessages.setLastSent(id);
    }

    public void protocolMessageReceived(String id) {
        protocolMessages.addReceived(id);
    }

    public void transitionProvisioningRequested() {
        transition(PROVISIONING_REQUESTED, PROVISIONING, INITIAL);
    }

    public void transitionProvisioned() {
        // requested is allowed to support retries
        transition(PROVISIONED, PROVISIONING, PROVISIONING_REQUESTED, PROVISIONED, REQUESTED);
    }

    public void transitionRequesting() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no REQUESTING state");
        }
        transition(REQUESTING, INITIAL, PROVISIONED, PROVISIONING_REQUESTED, REQUESTING);
    }

    public void transitionRequested() {
        if (Type.PROVIDER == type) {
            throw new IllegalStateException("Provider processes have no REQUESTED state");
        }
        transition(REQUESTED, PROVISIONED, REQUESTING, REQUESTED);

    }

    public void transitionStarting() {
        transition(STARTING, PROVISIONED, STARTING, SUSPENDED);
    }

    public boolean canBeStartedConsumer() {
        return currentStateIsOneOf(STARTED, REQUESTED, STARTING, RESUMED, SUSPENDED);
    }

    public void transitionStarted(String dataPlaneId) {
        if (type == CONSUMER) {
            transition(STARTED, state -> canBeStartedConsumer());
        } else {
            this.dataPlaneId = dataPlaneId;
            transition(STARTED, STARTED, STARTING, SUSPENDED, RESUMING);
        }
    }

    public boolean canBeCompleted() {
        return currentStateIsOneOf(COMPLETING, STARTED);
    }

    public void transitionCompleting() {
        transition(COMPLETING, state -> canBeCompleted());
    }

    public void transitionCompletingRequested() {
        transition(COMPLETING_REQUESTED, state -> canBeCompleted());
    }

    public void transitionCompleted() {
        transition(COMPLETED, COMPLETED, COMPLETING, COMPLETING_REQUESTED, STARTED);
    }

    public boolean canBeDeprovisioned() {
        return currentStateIsOneOf(COMPLETING, TERMINATING, COMPLETED, TERMINATED, DEPROVISIONING);
    }

    public void transitionDeprovisioning() {
        transition(DEPROVISIONING, state -> canBeDeprovisioned());
    }

    public void transitionDeprovisioningRequested() {
        transition(DEPROVISIONING_REQUESTED, DEPROVISIONING);
    }

    public void transitionDeprovisioned(String errorDetail) {
        this.errorDetail = errorDetail;
        transitionDeprovisioned();
    }

    public void transitionDeprovisioned() {
        transition(DEPROVISIONED, DEPROVISIONING, DEPROVISIONING_REQUESTED, DEPROVISIONED);
    }

    public boolean canBeTerminated() {
        return currentStateIsOneOf(INITIAL, PROVISIONING, PROVISIONING_REQUESTED, PROVISIONED, REQUESTING, REQUESTED,
                STARTING, STARTED, COMPLETING, COMPLETING_REQUESTED, SUSPENDING, SUSPENDING_REQUESTED, SUSPENDED,
                RESUMING, TERMINATING, TERMINATING_REQUESTED);
    }

    public void transitionTerminating(@Nullable String errorDetail) {
        this.errorDetail = errorDetail;
        transitionTerminating();
    }

    public void transitionTerminatingRequested(@Nullable String errorDetail) {
        this.errorDetail = errorDetail;
        transitionTerminatingRequested();
    }

    public void transitionTerminating() {
        transition(TERMINATING, state -> canBeTerminated());
    }

    public void transitionTerminatingRequested() {
        transition(TERMINATING_REQUESTED, state -> canBeTerminated());
    }

    public void transitionTerminated(String message) {
        this.errorDetail = message;
        transitionTerminated();
    }

    public void transitionTerminated() {
        transition(TERMINATED, state -> canBeTerminated());
    }

    public boolean canBeSuspended() {
        return currentStateIsOneOf(STARTED, SUSPENDING, SUSPENDING_REQUESTED);
    }

    public void transitionSuspending(String reason) {
        this.errorDetail = reason;
        transition(SUSPENDING, state -> canBeSuspended());
    }

    public void transitionSuspendingRequested(@Nullable String errorDetail) {
        this.errorDetail = errorDetail;
        transitionSuspendingRequested();
    }

    public void transitionSuspendingRequested() {
        transition(SUSPENDING_REQUESTED, state -> canBeSuspended());
    }

    public void transitionSuspended(String reason) {
        this.errorDetail = reason;
        transitionSuspended();
    }

    public void transitionResumed() {
        transition(RESUMED, state -> currentStateIsOneOf(RESUMING));
    }

    public void transitionResuming() {
        transition(RESUMING, state -> true);
    }

    public void transitionSuspended() {
        transition(SUSPENDED, state -> canBeSuspended());
    }

    public boolean currentStateIsOneOf(TransferProcessStates... states) {
        return Arrays.stream(states).map(TransferProcessStates::code).anyMatch(code -> code == state);
    }

    @JsonIgnore
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Set the correlationId, operation that's needed on the consumer side when it receives the first message with the
     * provider process id.
     *
     * @param correlationId the correlation id.
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @JsonIgnore
    public List<ProvisionedResource> getProvisionedResources() {
        return Optional.ofNullable(getProvisionedResourceSet()).map(ProvisionedResourceSet::getResources).orElse(emptyList());
    }

    @JsonIgnore
    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    /**
     * The transfer type to use for the requested data
     */
    @JsonIgnore
    public String getTransferType() {
        return transferType;
    }

    @JsonIgnore
    public String getAssetId() {
        return assetId;
    }

    @JsonIgnore
    public String getContractId() {
        return contractId;
    }

    @Nullable
    @JsonIgnore
    public DataAddress getDataDestination() {
        return dataDestination;
    }

    @JsonIgnore
    public String getProtocol() {
        return protocol;
    }

    @JsonIgnore
    public void updateDestination(DataAddress dataAddress) {
        this.dataDestination = dataAddress;
    }

    @JsonIgnore
    public String getDestinationType() {
        return dataDestination.getType();
    }

    public String getDataPlaneId() {
        return dataPlaneId;
    }

    public void setDataPlaneId(String dataPlaneId) {
        this.dataPlaneId = dataPlaneId;
    }

    @Override
    public TransferProcess copy() {
        var builder = Builder.newInstance()
                .resourceManifest(resourceManifest)
                .protocol(protocol)
                .correlationId(correlationId)
                .counterPartyAddress(counterPartyAddress)
                .dataDestination(dataDestination)
                .assetId(assetId)
                .contractId(contractId)
                .provisionedResourceSet(provisionedResourceSet)
                .contentDataAddress(contentDataAddress)
                .deprovisionedResources(deprovisionedResources)
                .privateProperties(privateProperties)
                .callbackAddresses(callbackAddresses)
                .transferType(transferType)
                .type(type)
                .protocolMessages(protocolMessages)
                .dataPlaneId(dataPlaneId);
        return copy(builder);
    }

    @Override
    public String stateAsString() {
        return TransferProcessStates.from(state).name();
    }

    public boolean suspensionWasRequestedByCounterParty() {
        return getState() == SUSPENDING_REQUESTED.code();
    }

    public boolean terminationWasRequestedByCounterParty() {
        return getState() == TERMINATING_REQUESTED.code();
    }

    public boolean completionWasRequestedByCounterParty() {
        return getState() == COMPLETING_REQUESTED.code();
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
        var that = (TransferProcess) o;
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

    private void transition(TransferProcessStates end, TransferProcessStates... starts) {
        transition(end, (state) -> Arrays.stream(starts).anyMatch(s -> s == state));
    }

    /**
     * Transition to a given end state if the passed predicate test correctly. Increases the
     * state count if transitioned to the same state and updates the state timestamp.
     *
     * @param end          The desired state.
     * @param canTransitTo Tells if the negotiation can transit to that state.
     */
    private void transition(TransferProcessStates end, Predicate<TransferProcessStates> canTransitTo) {
        var targetState = end.code();
        if (!canTransitTo.test(TransferProcessStates.from(state))) {
            throw new IllegalStateException(format("Cannot transition from state %s to %s", TransferProcessStates.from(state), TransferProcessStates.from(targetState)));
        }

        if (state != targetState) {
            protocolMessages.setLastSent(null);
        }

        transitionTo(targetState);
    }

    public enum Type {
        CONSUMER, PROVIDER
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends StatefulEntity.Builder<TransferProcess, Builder> {

        private Builder(TransferProcess process) {
            super(process);
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new TransferProcess());
        }

        public Builder type(Type type) {
            entity.type = type;
            return this;
        }

        public Builder resourceManifest(ResourceManifest manifest) {
            entity.resourceManifest = manifest;
            return this;
        }

        public Builder contentDataAddress(DataAddress dataAddress) {
            entity.contentDataAddress = dataAddress;
            return this;
        }

        public Builder provisionedResourceSet(ProvisionedResourceSet set) {
            entity.provisionedResourceSet = set;
            return this;
        }

        public Builder deprovisionedResources(List<DeprovisionedResource> resources) {
            entity.deprovisionedResources = resources;
            return this;
        }

        public Builder privateProperties(Map<String, Object> privateProperties) {
            entity.privateProperties = privateProperties;
            return this;
        }

        public Builder callbackAddresses(List<CallbackAddress> callbackAddresses) {
            entity.callbackAddresses = callbackAddresses;
            return this;
        }

        public Builder transferType(String transferType) {
            entity.transferType = transferType;
            return this;
        }

        public Builder protocolMessages(ProtocolMessages protocolMessages) {
            entity.protocolMessages = protocolMessages;
            return this;
        }

        public Builder dataPlaneId(String dataPlaneId) {
            entity.dataPlaneId = dataPlaneId;
            return this;
        }

        public Builder protocol(String protocol) {
            entity.protocol = protocol;
            return this;
        }

        public Builder correlationId(String correlationId) {
            entity.correlationId = correlationId;
            return this;
        }

        public Builder counterPartyAddress(String counterPartyAddress) {
            entity.counterPartyAddress = counterPartyAddress;
            return this;
        }

        public Builder dataDestination(DataAddress dataDestination) {
            entity.dataDestination = dataDestination;
            return this;
        }

        public Builder assetId(String assetId) {
            entity.assetId = assetId;
            return this;
        }

        public Builder contractId(String contractId) {
            entity.contractId = contractId;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public TransferProcess build() {
            super.build();

            if (entity.resourceManifest != null) {
                entity.resourceManifest.setTransferProcessId(entity.id);
            }

            if (entity.state == 0) {
                entity.transitionTo(INITIAL.code());
            }

            if (entity.callbackAddresses == null) {
                entity.callbackAddresses = new ArrayList<>();
            }

            return entity;
        }
    }
}
