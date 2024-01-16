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

package org.eclipse.edc.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.telemetry.TraceCarrier;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.Polymorphic;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A request to transfer data from a source to destination.
 */
@JsonTypeName("dataspaceconnector:dataflowrequest")
@JsonDeserialize(builder = DataFlowRequest.Builder.class)
public class DataFlowRequest implements Polymorphic, TraceCarrier {
    private String id;
    private String processId;

    private DataAddress sourceDataAddress;
    private DataAddress destinationDataAddress;
    private String transferType;
    private URI callbackAddress;
    private boolean trackable;

    private Map<String, String> properties = Map.of();
    private Map<String, String> traceContext = Map.of(); // TODO: should this stay in the DataFlow class?

    private DataFlowRequest() {
    }

    /**
     * The unique request id. Request ids are provided by the originating consumer and must be unique.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the process id this request is associated with.
     */
    public String getProcessId() {
        return processId;
    }

    /**
     * The source address of the data.
     */
    public DataAddress getSourceDataAddress() {
        return sourceDataAddress;
    }

    /**
     * The target address the data is to be sent to.
     */
    public DataAddress getDestinationDataAddress() {
        return destinationDataAddress;
    }

    /**
     * The transfer type to use for the request
     */
    public String getTransferType() {
        return transferType;
    }

    /**
     * Returns true if the request must be tracked for delivery guarantees.
     */
    public boolean isTrackable() {
        return trackable;
    }

    /**
     * Custom properties that are passed to the provider connector.
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Trace context for this request
     */
    @Override
    public Map<String, String> getTraceContext() {
        return traceContext;
    }

    /**
     * Callback address for this request once it has completed
     */
    public URI getCallbackAddress() { // TODO: this could be a URI
        return callbackAddress;
    }

    /**
     * A builder initialized with the current DataFlowRequest
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final DataFlowRequest request;

        private Builder() {
            this(new DataFlowRequest());
        }

        private Builder(DataFlowRequest request) {
            this.request = request;
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            request.id = id;
            return this;
        }

        public Builder processId(String id) {
            request.processId = id;
            return this;
        }

        public Builder destinationType(String type) {
            if (request.destinationDataAddress == null) {
                request.destinationDataAddress = DataAddress.Builder.newInstance().type(type).build();
            } else {
                request.destinationDataAddress.setType(type);
            }
            return this;
        }

        public Builder sourceDataAddress(DataAddress source) {
            request.sourceDataAddress = source;
            return this;
        }

        public Builder destinationDataAddress(DataAddress destination) {
            request.destinationDataAddress = destination;
            return this;
        }

        public Builder transferType(String transferType) {
            request.transferType = transferType;
            return this;
        }

        public Builder trackable(boolean value) {
            request.trackable = value;
            return this;
        }

        public Builder properties(Map<String, String> value) {
            request.properties = value == null ? null : Map.copyOf(value);
            return this;
        }

        public Builder traceContext(Map<String, String> value) {
            request.traceContext = value;
            return this;
        }

        public Builder callbackAddress(URI callbackAddress) {
            request.callbackAddress = callbackAddress;
            return this;
        }

        public DataFlowRequest build() {
            if (request.id == null) {
                request.id = UUID.randomUUID().toString();
            }
            Objects.requireNonNull(request.processId, "processId");
            Objects.requireNonNull(request.sourceDataAddress, "sourceDataAddress");
            Objects.requireNonNull(request.destinationDataAddress, "destinationDataAddress");
            Objects.requireNonNull(request.traceContext, "traceContext");
            return request;
        }

    }
}
