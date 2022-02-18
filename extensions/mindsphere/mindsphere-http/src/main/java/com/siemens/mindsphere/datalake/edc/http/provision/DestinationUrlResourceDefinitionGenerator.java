package com.siemens.mindsphere.datalake.edc.http.provision;

import com.siemens.mindsphere.datalake.edc.http.DataLakeSchema;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceDefinitionGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.Nullable;

import static java.util.UUID.randomUUID;

public class DestinationUrlResourceDefinitionGenerator implements ResourceDefinitionGenerator {
    public DestinationUrlResourceDefinitionGenerator(Monitor monitor) {
        this.monitor = monitor;
    }

    private Monitor monitor;

    @Override
    public @Nullable ResourceDefinition generate(TransferProcess process) {
        var request = process.getDataRequest();

        if (request.getDestinationType() == null) {
            return null;
        }

        final String dataDestinationType = request.getDataDestination().getType();
        if (!DataLakeSchema.TYPE.equals(dataDestinationType)) {
            return null;
        }

        monitor.info("Generating destination URL resource definition for process: " + process.getId());

        final String destinationPath = request.getDataDestination().getKeyName();

        return DestinationUrlResourceDefinition.Builder.newInstance()
                .id(randomUUID().toString())
                .path(destinationPath)
                .build();
    }
}
