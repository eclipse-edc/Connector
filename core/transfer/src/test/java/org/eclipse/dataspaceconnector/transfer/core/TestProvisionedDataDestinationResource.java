package org.eclipse.dataspaceconnector.transfer.core;

import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;

public class TestProvisionedDataDestinationResource extends ProvisionedDataDestinationResource {
    private final String resourceName;

    public TestProvisionedDataDestinationResource(String resourceName) {
        super();
        this.resourceName = resourceName;
    }

    @Override
    public DataAddress createDataDestination() {
        return null;
    }

    @Override
    public String getResourceName() {
        return resourceName;
    }
}
