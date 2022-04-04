package com.siemens.mindsphere.datalake.edc.http.provision;

import com.siemens.mindsphere.datalake.edc.http.DataLakeSchema;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.jetbrains.annotations.Nullable;

import static java.util.UUID.randomUUID;

public class DestinationUrlResourceDefinitionGenerator implements ConsumerResourceDefinitionGenerator {
    public DestinationUrlResourceDefinitionGenerator(Monitor monitor) {
        this.monitor = monitor;
    }

    private Monitor monitor;

    @Override
    public @Nullable ResourceDefinition generate(DataRequest dataRequest, Policy policy) {
        if (dataRequest.getDestinationType() == null) {
            return null;
        }

        final String dataDestinationType = dataRequest.getDataDestination().getType();
        if (!DataLakeSchema.TYPE.equals(dataDestinationType)) {
            return null;
        }

        monitor.info("Generating destination URL resource definition for dataRequest: " + dataRequest.getId());

        final String destinationPath = dataRequest.getDataDestination().getKeyName();

        return DestinationUrlResourceDefinition.Builder.newInstance()
                .id(randomUUID().toString())
                .path(destinationPath)
                .build();
    }

}
