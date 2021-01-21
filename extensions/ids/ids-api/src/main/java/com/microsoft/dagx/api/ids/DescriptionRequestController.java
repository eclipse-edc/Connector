package com.microsoft.dagx.api.ids;

import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import de.fraunhofer.iais.eis.DescriptionResponseMessageBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/ids")
public class DescriptionRequestController {

    @POST
    @Path("description")
    public DescriptionResponseMessage descriptionRequest(DescriptionRequestMessage request) {
        return new DescriptionResponseMessageBuilder().build();
    }
}
