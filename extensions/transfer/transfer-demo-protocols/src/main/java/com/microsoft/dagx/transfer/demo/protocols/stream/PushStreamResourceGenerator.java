package com.microsoft.dagx.transfer.demo.protocols.stream;

import com.microsoft.dagx.spi.transfer.provision.ResourceDefinitionGenerator;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import com.microsoft.dagx.transfer.demo.protocols.spi.DemoProtocols;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Generates resource definitions for push stream transfers. If the data destination is unmanaged (i.e. it is already created and managed independently) a definition will bot be
 * generated. Otherwise, a definition containing metadata to create a definition will be returned.
 */
public class PushStreamResourceGenerator implements ResourceDefinitionGenerator {
    private final String endpointAddress;

    public PushStreamResourceGenerator(String endpointAddress) {
        this.endpointAddress = endpointAddress;
    }

    @Override
    public @Nullable PushStreamResourceDefinition generate(TransferProcess process) {
        var dataRequest = process.getDataRequest();

        if (!DemoProtocols.PUSH_STREAM.equals(dataRequest.getDestinationType())) {
            return null;
        }
        if (!dataRequest.isManagedResources()) {
            // The resource is unmanaged, which means it was created by an external system. In this case it does not need to be provisioned.
            return null;
        }

        var destinationName = dataRequest.getDataDestination().getProperty(DemoProtocols.DESTINATION_NAME);
        if (destinationName == null) {
            destinationName = UUID.randomUUID().toString();
        }
        return PushStreamResourceDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .destinationName(destinationName)
                .endpointAddress(endpointAddress)
                .build();
    }
}
