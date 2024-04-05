
package org.eclipse.edc.connector.controlplane.services.transferprocess;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataAddressResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataDestinationResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedResource;

import java.util.List;
import java.util.Map;

public class TransferProcessServiceSubtypesProvider {
    public Map<Class<?>, List<Class<?>>> getSubtypes() {
        return Map.of(
                ProvisionedResource.class, List.of(ProvisionedDataAddressResource.class),
                ProvisionedDataAddressResource.class, List.of(ProvisionedDataDestinationResource.class, ProvisionedContentResource.class)
        );
    }
}