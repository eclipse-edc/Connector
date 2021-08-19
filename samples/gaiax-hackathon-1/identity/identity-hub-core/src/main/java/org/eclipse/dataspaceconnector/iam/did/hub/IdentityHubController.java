package org.eclipse.dataspaceconnector.iam.did.hub;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 *
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/identity-hub")
public class IdentityHubController {

    @GET
    @Path("health")
    public String stub() {
        return "";
    }
}
