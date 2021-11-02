package org.eclipse.dataspaceconnector.transfer.httproxy;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Objects;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/artifacts")
public class ForwardingController {

    private static final String PROPERTY_TARGET_URL = "targetUrl";
    private static final String PROPERTY_API_KEY_NAME = "apiKeyName";
    private final Monitor monitor;
    private final DataAddressResolver dataAddressResolver;
    private final Vault vault;
    private final OkHttpClient client;

    public ForwardingController(Monitor monitor, DataAddressResolver dataAddressResolver, Vault vault, OkHttpClient client) {
        this.monitor = monitor;
        this.dataAddressResolver = dataAddressResolver;
        this.vault = vault;
        this.client = client;
    }

    public String getBasePath() {
        return "/artifacts";
    }

    @GET
    @Path("/{id}")
    public Response getData(@HeaderParam("Authorization") String bearerToken, @PathParam("id") String assetId, @Context UriInfo uriInfo) throws MalformedURLException {
        monitor.info("Validate token for proxy");
        monitor.warning("Token validation not yet fully implemented!");
        if (bearerToken == null) {
            monitor.severe("No token supplied with request");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        monitor.info("resolving DataAddress for asset " + assetId);
        var dataAddress = dataAddressResolver.resolveForAsset(assetId);

        if (dataAddress == null) {
            monitor.warning("Asset with ID " + assetId + " not found");
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (dataAddress.getProperty(PROPERTY_TARGET_URL) != null) {
            var targetUrl = dataAddress.getProperty(PROPERTY_TARGET_URL);
            var secretName = dataAddress.getProperty(PROPERTY_API_KEY_NAME);

            var targetUri = addQueryParams(targetUrl, uriInfo.getQueryParameters());

            var request = new Request.Builder().get().url(targetUri.toURL());

            // attach an API key as auth header, if present
            if (secretName != null) {
                var apiKey = vault.resolveSecret(secretName);
                request.header("Authorization", Objects.requireNonNull(apiKey));
            }

            // add all query params of the incoming request to the outgoing one

            try (var response = client.newCall(request.build()).execute()) {
                if (response.isSuccessful()) {
                    return Response.ok(response.body().toString()).build();
                }
                return Response.status(response.code(), response.message()).build();
            } catch (IOException ex) {
                return Response.serverError().build();
            }
        } else {
            monitor.severe("A target URL for the requestes Asset does not exist");
        }

        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    private URI addQueryParams(String uri, MultivaluedMap<String, String> queryParameters) {
        var uriBuilder = UriBuilder.fromUri(uri);
        queryParameters.forEach((s, strings) -> uriBuilder.queryParam(s, strings.toArray()));
        return uriBuilder.build();

    }
}
