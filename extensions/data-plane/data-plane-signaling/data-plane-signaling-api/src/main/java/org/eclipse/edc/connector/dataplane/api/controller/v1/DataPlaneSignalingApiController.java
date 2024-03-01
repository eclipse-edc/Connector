/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.api.controller.v1;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/v1/dataflows")
public class DataPlaneSignalingApiController implements DataPlaneSignalingApi {

    private final TypeTransformerRegistry typeTransformerRegistry;
    private final DataPlaneAuthorizationService dataPlaneAuthorizationService;
    private final Monitor monitor;

    public DataPlaneSignalingApiController(TypeTransformerRegistry typeTransformerRegistry, DataPlaneAuthorizationService dataPlaneAuthorizationService, Monitor monitor) {
        this.typeTransformerRegistry = typeTransformerRegistry;
        this.dataPlaneAuthorizationService = dataPlaneAuthorizationService;
        this.monitor = monitor;
    }

    @POST
    @Override
    public void start(JsonObject dataFlowStartMessage, @Suspended AsyncResponse response) {
        var startMsg = typeTransformerRegistry.transform(dataFlowStartMessage, DataFlowStartMessage.class)
                .orElseThrow(InvalidRequestException::new);

        var dataAddress = dataPlaneAuthorizationService.createEndpointDataReference(startMsg);
        if (dataAddress.failed()) {
            monitor.warning("Error obtaining EDR DataAddress: " + dataAddress.getFailureDetail());
            response.resume(new InvalidRequestException(dataAddress.getFailure()));
        }

        var result = typeTransformerRegistry.transform(dataAddress.getContent(), JsonObject.class);
        result
                .onFailure(f -> {
                    monitor.warning("Error obtaining EDR DataAddress: " + result.getFailureDetail());
                    response.resume(new EdcException(f.getFailureDetail()));
                })
                .onSuccess(response::resume);
    }

    @GET
    @Path("/{id}/state")
    @Override
    public JsonObject getTransferState(@PathParam("id") String transferProcessId) {
        throw new UnsupportedOperationException();
    }

    @POST
    @Path("/{id}/terminate")
    @Override
    public void terminate(@PathParam("id") String transferProcessId, JsonObject terminationMessage) {
        throw new UnsupportedOperationException();
    }

    @POST
    @Path("/{id}/suspend")
    @Override
    public void suspend(@PathParam("id") String id, JsonObject suspendMessage) {
        throw new UnsupportedOperationException();
    }
}
