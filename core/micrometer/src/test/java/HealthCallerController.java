import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.io.IOException;

/**
 * This class provides an endpoint that will make the connector call the health endpoint.
 * This only purpose of this class is to be used for tests.
 * It helps testing Micrometer metrics like the OkHttpMetrics.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class HealthCallerController {

    private final Monitor monitor;
    private final OkHttpClient httpClient;

    public HealthCallerController(Monitor monitor, OkHttpClient httpClient) {

        this.monitor = monitor;
        this.httpClient = httpClient;
    }

    @GET
    @Path("callHealth")
    public String callHealth(@QueryParam("connectorUrl") String connectorUrl) throws IOException {
        String healthEndpoint = String.format("%s/api/check/health", connectorUrl);
        Request request = new Request.Builder().url(healthEndpoint).get().build();
        monitor.info("Sending a health request");
        return httpClient.newCall(request).execute().body().toString();
    }
}
