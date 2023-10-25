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
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.COMPLETED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.FAILED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.NOTIFIED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.RECEIVED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.STARTED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.TERMINATED;

/**
 * Entity that represent a Data Plane Transfer Flow
 * The id matches with the TransferProcess id that originated the DataFlow
 */
public class DataFlow extends StatefulEntity<DataFlow> {

    private DataAddress source;
    private DataAddress destination;
    private URI callbackAddress;
    private boolean trackable;
    private Map<String, String> properties = Map.of();

    @Override
    public DataFlow copy() {
        var builder = Builder.newInstance()
                .source(source)
                .destination(destination)
                .callbackAddress(callbackAddress)
                .trackable(trackable)
                .properties(properties);

        return copy(builder);
    }

    @Override
    public String stateAsString() {
        return DataFlowStates.from(state).name();
    }

    public DataAddress getSource() {
        return source;
    }

    public DataAddress getDestination() {
        return destination;
    }

    public URI getCallbackAddress() {
        return callbackAddress;
    }

    public boolean isTrackable() {
        return trackable;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public DataFlowRequest toRequest() {
        return DataFlowRequest.Builder.newInstance()
                .id(getId())
                .sourceDataAddress(getSource())
                .destinationDataAddress(getDestination())
                .processId(getId())
                .callbackAddress(getCallbackAddress())
                .traceContext(traceContext)
                .trackable(isTrackable())
                .properties(getProperties())
                .build();
    }

    public void transitToCompleted() {
        transitionTo(COMPLETED.code());
    }

    public void transitToReceived() {
        transitionTo(RECEIVED.code());
    }

    public void transitToFailed(String message) {
        errorDetail = message;
        transitionTo(FAILED.code());
    }

    public void transitToNotified() {
        transitionTo(NOTIFIED.code());
    }

    public void transitToTerminated() {
        transitionTo(TERMINATED.code());
    }

    public void transitionToStarted() {
        transitionTo(STARTED.code());
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

        public Builder trackable(boolean trackable) {
            entity.trackable = trackable;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            entity.properties = properties;
            return this;
        }

    }
}
