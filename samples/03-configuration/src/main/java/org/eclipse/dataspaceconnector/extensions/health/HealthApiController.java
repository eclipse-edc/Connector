package org.eclipse.dataspaceconnector.extensions.health;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import static java.lang.String.format;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class HealthApiController {

    private final Monitor monitor;
    private final String logPrefix;

    public HealthApiController(Monitor monitor, String logPrefix) {
        this.monitor = monitor;
        this.logPrefix = logPrefix;
    }

    @GET
    @Path("health")
    public String checkHealth() {
        monitor.info(format("%s :: Received a health request", logPrefix));
        return "{\"response\":\"I'm alive!\"}";
    }
}
