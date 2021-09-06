package org.eclipse.dataspaceconnector.api.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.common.collection.CollectionUtil;
import org.eclipse.dataspaceconnector.metadata.catalog.CatalogService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class ApiController {
    private final Monitor monitor;
    private final TransferProcessManager transferProcessManager;
    private final TransferProcessStore processStore;
    private final String connectorName;
    private final CatalogService catalogService;

    public ApiController(String connectorName, Monitor monitor, TransferProcessManager transferProcessManager, TransferProcessStore processStore, CatalogService catalogService) {
        this.connectorName = connectorName;
        this.monitor = monitor;
        this.transferProcessManager = transferProcessManager;
        this.processStore = processStore;
        this.catalogService = catalogService;
    }

    @GET
    @Path("health")
    public Response hello() {
        monitor.info("Controller says hello!");
        HashMap<String, String> m = formatAsJson("up and running");
        return Response.ok(m).build();
    }


    @GET
    @Path("catalog")
    public Response getCatalog(@QueryParam("connectorAddress") String connectorAddress) {
        monitor.info("catalog requested");
        var completable = catalogService.listArtifacts(connectorName, connectorAddress);

        return completable.thenApplyAsync(strings -> Response.ok(strings).build())
                .exceptionally(throwable -> Response.status(400).entity(throwable.getMessage()).build())
                .join();

    }

    @POST
    @Path("datarequest")
    public Response initiateDataRequest(DataRequest request) {
        if (request == null) {
            return Response.status(400).entity("data request cannot be null").build();
        }
        request = request.copy(UUID.randomUUID().toString()); //assign random ID
        monitor.info("Received new data request, ID = " + request.getId());
        var response = transferProcessManager.initiateClientRequest(request);
        monitor.info("Created new transfer process, ID = " + response.getId());

        ResponseStatus status = response.getStatus();
        if (status == ResponseStatus.OK) {
            return Response.ok(formatAsJson(response.getId())).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity(response.getStatus().name()).build();
        }
    }

    @GET
    @Path("datarequest/{id}")
    public Response getDatarequest(@PathParam("id") String requestId) {
        monitor.info("getting status of data request " + requestId);

        var process = processStore.find(requestId);
        if (process == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(process).build();
    }

    @DELETE
    @Path("datarequest/{id}")
    public Response deprovisionRequest(@PathParam("id") String requestId) {

        var process = processStore.find(requestId);
        if (process == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            if (CollectionUtil.isAnyOf(process.getState(),
                    TransferProcessStates.DEPROVISIONED.code(),
                    TransferProcessStates.DEPROVISIONING_REQ.code(),
                    TransferProcessStates.DEPROVISIONING.code(),
                    TransferProcessStates.ENDED.code()
            )) {
                monitor.info("Request already deprovisioning or deprovisioned.");
            } else {
                monitor.info("starting to deprovision data request " + requestId);
                process.transitionCompleted();
                process.transitionDeprovisionRequested();
                processStore.update(process);
            }
            return Response.ok(formatAsJson(TransferProcessStates.from(process.getState()).toString())).build();
        } catch (IllegalStateException ex) {
            monitor.severe(ex.getMessage());
            return Response.status(400).entity("The process must be in one of these states: " + String.join(", ", TransferProcessStates.IN_PROGRESS.name(), TransferProcessStates.REQUESTED_ACK.name(), TransferProcessStates.STREAMING.name())).build();
        }

    }

    @GET
    @Path("datarequest/{id}/state")
    public Response getStatus(@PathParam("id") String requestId) {
        monitor.info("getting status of data request " + requestId);

        var process = processStore.find(requestId);
        if (process == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(formatAsJson(TransferProcessStates.from(process.getState()).toString())).build();
    }

    @NotNull
    private HashMap<String, String> formatAsJson(String simpleValue) {
        var m = new HashMap<String, String>();
        m.put("response", simpleValue);
        return m;
    }
}
