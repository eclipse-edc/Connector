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
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.Polymorphic;

import java.util.Map;
import java.util.Objects;

/**
 * A request to transfer data from a source to destination.
 */
@JsonTypeName("dataspaceconnector:dataflowrequest")
@JsonDeserialize(builder = DataFlowRequest.Builder.class)
public class DataFlowRequest implements Polymorphic {
    private String id;
    private String processId;

    private DataAddress sourceDataAddress;
    private DataAddress destinationDataAddress;

    private boolean trackable;

    private Map<String, String> properties = Map.of();

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

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final DataFlowRequest request;

        private Builder() {
            request = new DataFlowRequest();
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

        public Builder sourceDataAddress(DataAddress destination) {
            request.sourceDataAddress = destination;
            return this;
        }

        public Builder destinationDataAddress(DataAddress destination) {
            request.destinationDataAddress = destination;
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

        public DataFlowRequest build() {
            Objects.requireNonNull(request.processId, "processId");
            Objects.requireNonNull(request.sourceDataAddress, "sourceDataAddress");
            Objects.requireNonNull(request.destinationDataAddress, "destinationDataAddress");
            return request;
        }

    }
}
