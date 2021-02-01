package com.microsoft.dagx.ids.api.transfer;

import com.microsoft.dagx.spi.monitor.Monitor;
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/ids")
public class ArtifactRequestController {
    private Monitor monitor;

    public ArtifactRequestController(Monitor monitor) {
        this.monitor = monitor;
    }

    @POST
    @Path("request")
    public Response request(ArtifactRequestMessage message) {
        monitor.debug(() -> "Received artifact request for: " + message.getRequestedArtifact());
        return Response.ok().build();
    }


}
