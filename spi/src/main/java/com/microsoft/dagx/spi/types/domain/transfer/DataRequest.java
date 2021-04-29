package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.microsoft.dagx.spi.types.domain.Polymorphic;
import com.microsoft.dagx.spi.types.domain.message.RemoteMessage;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;

/**
 * Polymorphic data request.
 */
@JsonTypeName("dagx:datarequest")
@JsonDeserialize(builder = DataRequest.Builder.class)
public class DataRequest implements RemoteMessage, Polymorphic {
    private String id;

    private String connectorAddress;

    private String protocol;

    private String connectorId;

    private DataEntry<?> dataEntry;

    private DataAddress dataAddress;

    private String destinationType;

    private boolean managedResources = true;


    /**
     * The unique request id.
     */
    public String getId() {
        return id;
    }

    /**
     * The protocol-specific address of the provider connector.
     */
    public String getConnectorAddress() {
        return connectorAddress;
    }

    /**
     * The protocol over which the data request is sent to the provider connector.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * The provider connector id.
     */
    public String getConnectorId() {
        return connectorId;
    }

    public DataEntry<?> getDataEntry() {
        return dataEntry;
    }

    public String getDestinationType() {
        return destinationType;
    }

    /**
     * The target address the data is to be sent to. Set by the request originator, e.g., the client connector.
     */
    public DataAddress getDataDestination() {
        return dataAddress;
    }

    public boolean isManagedResources() {
        return managedResources;
    }


    private DataRequest() {
    }

    public void updateDestination(DataAddress dataAddress) {
        this.dataAddress = dataAddress;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final DataRequest request;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            request.id = id;
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

        public Builder connectorId(String connectorId) {
            request.connectorId = connectorId;
            return this;
        }

        public Builder dataEntry(DataEntry<?> entry) {
            request.dataEntry = entry;
            return this;
        }

        public Builder destinationType(String type) {
            request.destinationType = type;
            return this;
        }

        public Builder dataDestination(DataAddress destination) {
            request.dataAddress = destination;
            return this;
        }

        public Builder managedResources(boolean value) {
            request.managedResources = value;
            return this;
        }

        private Builder() {
            request = new DataRequest();
        }

        public DataRequest build() {
            if (request.dataAddress == null && request.destinationType == null) {
                throw new IllegalArgumentException("A data destination or type must be specified");
            }
            return request;
        }

    }
}
