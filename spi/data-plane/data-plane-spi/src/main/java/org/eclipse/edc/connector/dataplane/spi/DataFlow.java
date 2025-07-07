/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.dataplane.spi.provision.DeprovisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.COMPLETED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.DEPROVISIONED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.DEPROVISIONING;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.DEPROVISION_FAILED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.DEPROVISION_REQUESTED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.FAILED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.NOTIFIED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.PROVISIONED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.PROVISIONING;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.PROVISION_NOTIFYING;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.PROVISION_REQUESTED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.RECEIVED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.STARTED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.SUSPENDED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.TERMINATED;

/**
 * Entity that represent a Data Plane Transfer Flow
 * The id matches with the TransferProcess id that originated the DataFlow
 */
public class DataFlow extends StatefulEntity<DataFlow> {

    public static final String TERMINATION_REASON = "terminationReason";
    private static final String PARTICIPANT_ID_PROPERTY = "participantId";
    private static final String ASSET_ID_PROPERTY = "assetId";
    private static final String AGREEMENT_ID_PROPERTY = "agreementId";
    private DataAddress source;
    private DataAddress destination;
    private URI callbackAddress;
    private Map<String, String> properties = new HashMap<>();
    private TransferType transferType;
    private String runtimeId;
    private final List<ProvisionResource> resourceDefinitions = new ArrayList<>();

    @Override
    public DataFlow copy() {
        var builder = Builder.newInstance()
                .source(source)
                .destination(destination)
                .callbackAddress(callbackAddress)
                .properties(properties)
                .transferType(getTransferType())
                .runtimeId(runtimeId)
                .resourceDefinitions(resourceDefinitions);

        return copy(builder);
    }

    @Override
    public String stateAsString() {
        return DataFlowStates.from(state).name();
    }

    public DataAddress getSource() {
        var provisioned = provisionedDataAddress();
        if (provisioned != null) {
            return provisioned;
        }

        return source;
    }

    public DataAddress getDestination() {
        return destination;
    }

    public URI getCallbackAddress() {
        return callbackAddress;
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public String getRuntimeId() {
        return runtimeId;
    }

    public DataFlowStartMessage toRequest() {
        return DataFlowStartMessage.Builder.newInstance()
                .id(getId())
                .sourceDataAddress(getSource())
                .destinationDataAddress(getDestination())
                .processId(getId())
                .callbackAddress(getCallbackAddress())
                .traceContext(traceContext)
                .properties(getProperties())
                .transferType(getTransferType())
                .build();
    }

    public void transitionToProvisioning() {
        transitionTo(PROVISIONING.code());
    }

    public void transitionToProvisionNotifying() {
        transitionTo(PROVISION_NOTIFYING.code());
    }

    public void transitionToProvisioned() {
        transitionTo(PROVISIONED.code());
    }

    public void transitionToDeprovisioning() {
        transitionTo(DEPROVISIONING.code());
    }

    public void transitToCompleted() {
        transitionTo(COMPLETED.code());
    }

    public void transitToReceived(String runtimeId) {
        this.runtimeId = runtimeId;
        transitionTo(RECEIVED.code());
    }

    public void transitToFailed(String message) {
        errorDetail = message;
        transitionTo(FAILED.code());
    }

    public void transitionToNotified() {
        transitionTo(NOTIFIED.code());
    }

    public void transitToTerminated(@Nullable String reason) {
        transitionTo(TERMINATED.code());
        if (reason != null) {
            properties.put(TERMINATION_REASON, reason);
        }
    }

    public void transitionToStarted(String runtimeId) {
        this.runtimeId = runtimeId;
        transitionTo(STARTED.code());
    }

    public void transitToSuspended() {
        transitionTo(SUSPENDED.code());
    }

    public List<ProvisionResource> getResourceDefinitions() {
        return resourceDefinitions;
    }

    public void transitionToDeprovisionFailed() {
        transitionTo(DEPROVISION_FAILED.code());
    }

    public void addResourceDefinitions(List<ProvisionResource> definitions) {
        resourceDefinitions.addAll(definitions);
    }

    public boolean isProvisionCompleted() {
        return resourceDefinitions.isEmpty() || resourceDefinitions.stream().allMatch(ProvisionResource::isProvisioned);
    }

    public boolean isProvisionRequested() {
        return resourceDefinitions.stream().filter(it -> !it.isProvisioned()).allMatch(ProvisionResource::isProvisionRequested);
    }

    public boolean isDeprovisionCompleted() {
        return resourceDefinitions.stream().allMatch(ProvisionResource::isDeprovisioned);
    }

    public boolean isDeprovisionRequested() {
        return resourceDefinitions.stream().filter(it -> !it.isDeprovisioned()).allMatch(ProvisionResource::isDeprovisionRequested);
    }

    public List<ProvisionResource> resourcesToBeProvisioned() {
        return resourceDefinitions.stream().filter(ProvisionResource::hasToBeProvisioned).toList();
    }

    public List<ProvisionResource> resourcesToBeDeprovisioned() {
        return resourceDefinitions.stream().filter(ProvisionResource::hasToBeDeprovisioned).toList();
    }

    public void resourceProvisioned(List<ProvisionedResource> provisionedResources) {
        provisionedResources.forEach(provisionedResource -> {
            resourceDefinitions.stream()
                    .filter(resource -> resource.getId().equals(provisionedResource.getId()))
                    .findAny()
                    .ifPresent(resource -> resource.transitionProvisioned(provisionedResource));
        });

        if (isProvisionCompleted()) {
            transitionTo(PROVISION_NOTIFYING.code());
        } else if (isProvisionRequested()) {
            transitionTo(PROVISION_REQUESTED.code());
        }

    }

    public void resourceDeprovisioned(List<DeprovisionedResource> deprovisionedResources) {
        deprovisionedResources.forEach(deprovisionedResource -> {
            resourceDefinitions.stream()
                    .filter(resource -> resource.getId().equals(deprovisionedResource.getId()))
                    .findAny()
                    .ifPresent(resource -> resource.transitionDeprovisioned(deprovisionedResource));
        });

        if (isDeprovisionCompleted()) {
            transitionTo(DEPROVISIONED.code());
        } else if (isDeprovisionRequested()) {
            transitionTo(DEPROVISION_REQUESTED.code());
        }
    }

    public DataAddress provisionedDataAddress() {
        return resourceDefinitions.stream()
                .map(ProvisionResource::getProvisionedResource)
                .map(ProvisionedResource::getDataAddress)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    public boolean isConsumer() {
        return source == null;
    }

    public String getParticipantId() {
        return properties.get(PARTICIPANT_ID_PROPERTY);
    }

    public String getAssetId() {
        return properties.get(ASSET_ID_PROPERTY);
    }

    public String getAgreementId() {
        return properties.get(AGREEMENT_ID_PROPERTY);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends StatefulEntity.Builder<DataFlow, Builder> {

        private Builder(DataFlow process) {
            super(process);
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new DataFlow());
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public DataFlow build() {
            super.build();

            if (entity.id == null) {
                entity.id = UUID.randomUUID().toString();
            }

            return entity;
        }

        public Builder source(DataAddress source) {
            entity.source = source;
            return this;
        }

        public Builder destination(DataAddress destination) {
            entity.destination = destination;
            return this;
        }

        public Builder callbackAddress(URI callbackAddress) {
            entity.callbackAddress = callbackAddress;
            return this;
        }

        public Builder transferType(TransferType transferType) {
            entity.transferType = transferType;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            entity.properties = new HashMap<>(properties);
            return this;
        }

        public Builder runtimeId(String runtimeId) {
            entity.runtimeId = runtimeId;
            return this;
        }

        public Builder resourceDefinitions(List<ProvisionResource> resourceDefinitions) {
            entity.resourceDefinitions.addAll(resourceDefinitions);
            return this;
        }

        public Builder participantId(String participantId) {
            entity.properties.put(PARTICIPANT_ID_PROPERTY, participantId);
            return this;
        }

        public Builder assetId(String assetId) {
            entity.properties.put(ASSET_ID_PROPERTY, assetId);
            return this;
        }

        public Builder agreementId(String agreementId) {
            entity.properties.put(AGREEMENT_ID_PROPERTY, agreementId);
            return this;
        }
    }
}
