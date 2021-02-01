package com.microsoft.dagx.ids.api.transfer;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.TransferManager;
import com.microsoft.dagx.spi.transfer.TransferManagerRegistry;
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

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

        // TODO validate URN scheme
        TransferManager transferManager = transferManagerRegistry.getManager(dataUrn);

        return Response.ok().build();
    }


}
