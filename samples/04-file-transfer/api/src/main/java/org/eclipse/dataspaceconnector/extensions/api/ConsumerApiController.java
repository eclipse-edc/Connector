package org.eclipse.dataspaceconnector.extensions.api;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class ConsumerApiController {

    private final Monitor monitor;

    public ConsumerApiController(Monitor monitor) {
        this.monitor = monitor;
    }

    @GET
    @Path("health")
    public String checkHealth() {
        monitor.info("%s :: Received a health request");
        return "I'm alive!";
    }
}
