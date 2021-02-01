package com.microsoft.dagx.ids.api.transfer;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.TransferManager;
import com.microsoft.dagx.spi.transfer.TransferManagerRegistry;
import com.microsoft.dagx.spi.transfer.TransferResponse;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.UUID;

/**
 * Receives incoming data transfer requests and processes them.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/ids")
public class ArtifactRequestController {
    private TransferManagerRegistry transferManagerRegistry;
    private Monitor monitor;

    public ArtifactRequestController(TransferManagerRegistry transferManagerRegistry, Monitor monitor) {
        this.transferManagerRegistry = transferManagerRegistry;
        this.monitor = monitor;
    }

    @POST
    @Path("request")
    public Response request(ArtifactRequestMessage message) {
        URI dataUrn = message.getRequestedArtifact();
        monitor.debug(() -> "Received artifact request for: " + dataUrn);

        // TODO validate URN scheme and enforce policy

        TransferManager transferManager = transferManagerRegistry.getManager(dataUrn);
        if (transferManager == null) {
            // TODO this needs to return the proper IDS message
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        TransferResponse response = transferManager.initiateTransfer(DataRequest.Builder.newInstance(UUID.randomUUID().toString()).build());

        monitor.info("Data transfer request initiated");
        
        return Response.ok().build();
    }


}
