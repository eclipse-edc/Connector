package org.eclipse.edc.transfer.demo.protocols.stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.transfer.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.edc.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.edc.transfer.demo.protocols.spi.DemoProtocols;

import java.util.Objects;

/**
 * Defines a streaming destination topic to be provisioned.
 */
@JsonDeserialize(builder = PushStreamProvisionedResourceDefinition.Builder.class)
@JsonTypeName("edc:pushstreamprovisionedresource")
public class PushStreamProvisionedResourceDefinition extends ProvisionedDataDestinationResource {
    private String endpointAddress;
    private String destinationName;

    private PushStreamProvisionedResourceDefinition() {
    }

    public String getEndpointAddress() {
        return endpointAddress;
    }

    public String getDestinationName() {
        return destinationName;
    }

    @Override
    public String getResourceName() {
        return getDestinationName();
    }

    @Override
    public DataAddress createDataDestination() {
        return DataAddress.Builder.newInstance()
                .type(DemoProtocols.PUSH_STREAM_WS)
                .keyName("demo-temp-" + destinationName)
                .property(DemoProtocols.ENDPOINT_ADDRESS, endpointAddress)
                .property(DemoProtocols.DESTINATION_NAME, destinationName)
                .build();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedResource.Builder<PushStreamProvisionedResourceDefinition, Builder> {

        private Builder() {
            super(new PushStreamProvisionedResourceDefinition());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder endpointAddress(String endpointAddress) {
            provisionedResource.endpointAddress = endpointAddress;
            return this;
        }

        public Builder destinationName(String destinationName) {
            provisionedResource.destinationName = destinationName;
            return this;
        }

        @Override
        public void verify() {
            Objects.requireNonNull(provisionedResource.endpointAddress, "endpointAddress");
            Objects.requireNonNull(provisionedResource.destinationName, "destinationName");
        }
    }

}
