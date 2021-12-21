package org.eclipse.dataspaceconnector.transfer.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;

public class TestProvisionedDataDestinationResource extends ProvisionedDataDestinationResource {
    protected TestProvisionedDataDestinationResource() {
        super();
    }

    @Override
    public DataAddress createDataDestination() {
        return null;
    }

    @Override
    public String getResourceName() {
        return "test-resource";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedDataDestinationResource.Builder<TestProvisionedDataDestinationResource, Builder> {

        private Builder() {
            super(new TestProvisionedDataDestinationResource());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

    }
}
