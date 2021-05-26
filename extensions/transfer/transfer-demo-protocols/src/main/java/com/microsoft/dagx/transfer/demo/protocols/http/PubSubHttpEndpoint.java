package com.microsoft.dagx.transfer.demo.protocols.http;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 *
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/demo/pubsub")
public class PubSubHttpEndpoint {

    @POST
    @Path("{destinationName}")
    public void publish(@PathParam("destinationName") String destinationName) {

    }

}
