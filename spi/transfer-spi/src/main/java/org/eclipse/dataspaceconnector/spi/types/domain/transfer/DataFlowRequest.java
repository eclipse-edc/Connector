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
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;

import java.util.Map;
import java.util.Objects;

/**
 * A request to transfer data from a source to destination.
 */
@JsonTypeName("dataspaceconnector:dataflowrequest")
@JsonDeserialize(builder = DataFlowRequest.Builder.class)
public class DataFlowRequest implements RemoteMessage, Polymorphic {
    private String id;
    private String processId;
    private String protocol;

    private DataAddress sourceDataAddress;
    private DataAddress destinationDataAddress;

    private Map<String, String> properties = Map.of();

    private TransferType transferType;

    private DataFlowRequest() {
        transferType = new TransferType();
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
     * The protocol over which the data request is sent to the provider connector.
     */
    @Override
    public String getProtocol() {
        return protocol;
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
     * Custom properties that are passed to the provider connector.
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    public TransferType getTransferType() {
        return transferType;
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

        public Builder protocol(String protocol) {
            request.protocol = protocol;
            return this;
        }

        public Builder destinationType(String type) {
            if (request.destinationDataAddress == null) {
                request.destinationDataAddress = DataAddress.Builder.newInstance()
                        .type(type).build();
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

        public Builder properties(Map<String, String> value) {
            request.properties = value == null ? null : Map.copyOf(value);
            return this;
        }

        public DataFlowRequest build() {
            Objects.requireNonNull(request.processId, "processId");
            Objects.requireNonNull(request.sourceDataAddress, "sourceDataAddress");
            Objects.requireNonNull(request.destinationDataAddress, "destinationDataAddress");
            Objects.requireNonNull(request.transferType, "transferType");
            return request;
        }

        private Builder dataAddress(DataAddress dataAddress) {
            request.destinationDataAddress = dataAddress;
            return this;
        }

        public Builder transferType(TransferType transferType) {
            request.transferType = transferType;
            return this;
        }

    }
}
