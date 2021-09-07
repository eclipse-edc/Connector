package org.eclipse.dataspaceconnector.extensions.api;


import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class ConsumerApiController {

    private final Monitor monitor;
    private final TransferProcessManager processManager;

    public ConsumerApiController(Monitor monitor, TransferProcessManager processManager) {
        this.monitor = monitor;
        this.processManager = processManager;
    }

    @GET
    @Path("health")
    public String checkHealth() {
        monitor.info("%s :: Received a health request");
        return "I'm alive!";
    }

    @POST
    @Path("file/{filename}")
    public Response startTransfer(@PathParam("filename") String filename, @QueryParam("connectorAddress") String connectorAddress,
                                  @QueryParam("destination") String destinationPath) {

        monitor.info(format("Received request for file %s against provider %s", filename, connectorAddress));

        Objects.requireNonNull(filename, "filename");
        Objects.requireNonNull(connectorAddress, "connectorAddress");

        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .connectorAddress(connectorAddress)
                .protocol("ids-rest")
                .connectorId("consumer")
                .dataEntry(DataEntry.Builder.newInstance()
                        .id(filename)
                        .policyId("use-eu")
                        .build())
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("File")
                        .property("path", destinationPath)
                        .build())
                .managedResources(false)
                .build();

        var response = processManager.initiateConsumerRequest(dataRequest);

        return response.getStatus() != ResponseStatus.OK ? Response.status(400).build() : Response.ok(response.getId()).build();

    }
}
