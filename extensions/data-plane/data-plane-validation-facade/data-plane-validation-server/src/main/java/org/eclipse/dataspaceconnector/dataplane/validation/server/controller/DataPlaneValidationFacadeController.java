package org.eclipse.dataspaceconnector.dataplane.validation.server.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.token.spi.TokenValidationService;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class DataPlaneValidationFacadeController {

    private final TokenValidationService tokenValidationService;

    public DataPlaneValidationFacadeController(TokenValidationService tokenValidationService) {
        this.tokenValidationService = tokenValidationService;
    }

    @GET
    @Path("validate/{token}")
    public Response validate(@PathParam("token") String token) {
        Result<ClaimToken> result = tokenValidationService.validate(token);
        if (result.failed()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Token validation failed: " + String.join(", ", result.getFailureMessages()))
                    .build();
        }
        return Response.ok(result.getContent()).build();
    }
}
