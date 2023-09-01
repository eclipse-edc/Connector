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

package org.eclipse.edc.connector.transfer.spi.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.Polymorphic;

/**
 * Polymorphic data request.
 */
@JsonTypeName("dataspaceconnector:datarequest")
@JsonDeserialize(builder = DataRequest.Builder.class)
public class DataRequest implements Polymorphic {
    private String id;
    private String processId;
    private String connectorAddress;
    private String protocol;
    private String assetId;
    private String contractId;
    private DataAddress dataDestination;

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
     * Associates the request with a process id.
     */
    void associateWithProcessId(String processId) {
        this.processId = processId;
    }

    /**
     * The protocol over which the data request is sent to the provider connector.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * The protocol-specific address of the other connector.
     */
    public String getConnectorAddress() {
        return connectorAddress;
    }

    /**
     * The id of the requested asset.
     */
    public String getAssetId() {
        return assetId;
    }

    /**
     * The id of the requested contract.
     */
    public String getContractId() {
        return contractId;
    }

    /**
     * The type of destination the requested data should be routed to.
     */
    public String getDestinationType() {
        return dataDestination != null ? dataDestination.getType() : null;
    }

    /**
     * The target address the data is to be sent to. Set by the request originator, e.g., the consumer connector.
     */
    public DataAddress getDataDestination() {
        return dataDestination;
    }

    public void updateDestination(DataAddress dataAddress) {
        dataDestination = dataAddress;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final DataRequest request;

        private Builder() {
            request = new DataRequest();
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

        public Builder connectorAddress(String address) {
            request.connectorAddress = address;
            return this;
        }

        public Builder protocol(String protocol) {
            request.protocol = protocol;
            return this;
        }

        public Builder contractId(String contractId) {
            request.contractId = contractId;
            return this;
        }

        public Builder assetId(String assetId) {
            request.assetId = assetId;
            return this;
        }

        public Builder destinationType(String type) {
            if (request.dataDestination == null) {
                request.dataDestination = DataAddress.Builder.newInstance()
                        .type(type).build();
            } else {
                request.dataDestination.setType(type);
            }
            return this;
        }

        public Builder dataDestination(DataAddress destination) {
            request.dataDestination = destination;
            return this;
        }

        public DataRequest build() {
            if (request.dataDestination == null && request.getDestinationType() == null) {
                throw new IllegalArgumentException("A data destination or type must be specified");
            }
            return request;
        }

    }
}
