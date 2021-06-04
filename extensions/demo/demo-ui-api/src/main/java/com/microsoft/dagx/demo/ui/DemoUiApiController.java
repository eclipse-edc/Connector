/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.demo.ui;

import com.microsoft.dagx.spi.message.RemoteMessageDispatcherRegistry;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.metadata.QueryRequest;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.microsoft.dagx.common.Cast.cast;

/**
 *
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/demo-ui")
public class DemoUiApiController {
    private static final String PREFIX = "http://";

    private final RemoteMessageDispatcherRegistry dispatcherRegistry;
    private final TransferProcessManager processManager;

    private final Monitor monitor;

    public DemoUiApiController(RemoteMessageDispatcherRegistry dispatcherRegistry, TransferProcessManager processManager, Monitor monitor) {
        this.dispatcherRegistry = dispatcherRegistry;
        this.processManager = processManager;
        this.monitor = monitor;
    }

    @GET
    @Path("artifacts/{connector}")
    public Response getArtifacts(@PathParam("connector") String connector) {
        try {
            var query = QueryRequest.Builder.newInstance()
                    .connectorAddress(PREFIX + connector)
                    .connectorId(connector)
                    .queryLanguage("dagx")
                    .query("select *")
                    .protocol("ids-rest").build();

            CompletableFuture<List<String>> future = cast(dispatcherRegistry.send(List.class, query, () -> null));

            var artifacts = future.get();
            return Response.ok().entity(artifacts).build();

        } catch (InterruptedException | ExecutionException e) {
            monitor.severe("Error serving request", e);
            return Response.serverError().build();
        }

    }

    @POST
    @Path("data/request")
    public Response initiateDataRequest(Map<String, String> request) {
        var connector = (String) request.get("connector");
        var artifact = (String) request.get("artifact");
        var usRequest = createRequest(connector, UUID.randomUUID().toString(), DataEntry.Builder.newInstance().id(artifact).build());

        processManager.initiateClientRequest(usRequest);
        return Response.ok().build();

    }

    private DataRequest createRequest(String connector, String id, DataEntry<?> artifactId) {
        return DataRequest.Builder.newInstance()
                .id(id)
                .protocol("ids-rest")
                .dataEntry(artifactId)
                .connectorId(connector)
                .connectorAddress(PREFIX + connector)
                .destinationType("dagx:s3").build();
    }


}
