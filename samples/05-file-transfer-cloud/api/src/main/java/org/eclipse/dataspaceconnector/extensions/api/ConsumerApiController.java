package org.eclipse.dataspaceconnector.extensions.api;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.common.collection.CollectionUtil;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

import java.util.UUID;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class ConsumerApiController {

    private final Monitor monitor;
    private final TransferProcessManager processManager;
    private final TransferProcessStore processStore;

    public ConsumerApiController(Monitor monitor, TransferProcessManager processManager, TransferProcessStore processStore) {
        this.monitor = monitor;
        this.processManager = processManager;
        this.processStore = processStore;
    }

    @GET
    @Path("health")
    public String checkHealth() {
        monitor.info("%s :: Received a health request");
        return "{\"response\":\"I'm alive!\"}";
    }

    @POST
    @Path("datarequest")
    public Response initiateDataRequest(DataRequest request) {
        if (request == null) {
            return Response.status(400).entity("data request cannot be null").build();
        }
        request = request.copy(UUID.randomUUID().toString()); //assign random ID
        monitor.info("Received new data request, ID = " + request.getId());
        var response = processManager.initiateConsumerRequest(request);
        monitor.info("Created new transfer process, ID = " + response.getId());

        ResponseStatus status = response.getStatus();
        if (status == ResponseStatus.OK) {
            return Response.ok(response.getId()).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity(response.getStatus().name()).build();
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
        return Response.ok(TransferProcessStates.from(process.getState()).toString()).build();
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
            return Response.ok(TransferProcessStates.from(process.getState()).toString()).build();
        } catch (IllegalStateException ex) {
            monitor.severe(ex.getMessage());
            return Response.status(400).entity("The process must be in one of these states: " +
                    String.join(", ", TransferProcessStates.IN_PROGRESS.name(), TransferProcessStates.REQUESTED_ACK.name(), TransferProcessStates.COMPLETED.name(), TransferProcessStates.STREAMING.name())).build();
        }

    }


}
