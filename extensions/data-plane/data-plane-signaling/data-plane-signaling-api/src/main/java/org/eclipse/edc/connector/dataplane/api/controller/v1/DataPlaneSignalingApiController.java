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
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/v1/dataflows")
public class DataPlaneSignalingApiController implements DataPlaneSignalingApi {

    private final TypeTransformerRegistry typeTransformerRegistry;
    private final DataPlaneAuthorizationService dataPlaneAuthorizationService;
    private final DataPlaneManager dataPlaneManager;
    private final Monitor monitor;

    public DataPlaneSignalingApiController(TypeTransformerRegistry typeTransformerRegistry, DataPlaneAuthorizationService dataPlaneAuthorizationService, DataPlaneManager dataPlaneManager, Monitor monitor) {
        this.typeTransformerRegistry = typeTransformerRegistry;
        this.dataPlaneAuthorizationService = dataPlaneAuthorizationService;
        this.dataPlaneManager = dataPlaneManager;
        this.monitor = monitor;
    }

    @POST
    @Override
    public JsonObject start(JsonObject dataFlowStartMessage) {
        var startMsg = typeTransformerRegistry.transform(dataFlowStartMessage, DataFlowStartMessage.class)
                .onFailure(f -> monitor.warning("Error transforming %s: %s".formatted(DataFlowStartMessage.class, f.getFailureDetail())))
                .orElseThrow(InvalidRequestException::new);

        dataPlaneManager.validate(startMsg)
                .onFailure(f -> monitor.warning("Failed to validate request: " + f.getFailureDetail()))
                .orElseThrow(f -> f.getMessages().isEmpty() ?
                        new InvalidRequestException("Failed to validate request: %s".formatted(startMsg.getId())) :
                        new InvalidRequestException(f.getMessages()));

        monitor.debug("Create EDR");
        var dataAddress = dataPlaneAuthorizationService.createEndpointDataReference(startMsg)
                .onFailure(f -> monitor.warning("Error obtaining EDR DataAddress: " + f.getFailureDetail()))
                .orElseThrow(InvalidRequestException::new);

        dataPlaneManager.initiate(startMsg);

        return typeTransformerRegistry.transform(dataAddress, JsonObject.class)
                .onFailure(f -> monitor.warning("Error obtaining EDR DataAddress: " + f.getFailureDetail()))
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    @GET
    @Path("/{id}/state")
    @Override
    public JsonObject getTransferState(@PathParam("id") String transferProcessId) {
        var state = dataPlaneManager.transferState(transferProcessId);
        return null;
    }

    @POST
    @Path("/{id}/terminate")
    @Override
    public void terminate(@PathParam("id") String dataFlowId, JsonObject terminationMessage) {

        var msg = typeTransformerRegistry.transform(terminationMessage, DataFlowTerminateMessage.class)
                .onFailure(f -> monitor.warning("Error transforming %s: %s".formatted(DataFlowTerminateMessage.class, f.getFailureDetail())))
                .orElseThrow(InvalidRequestException::new);

        // todo: add msg.reason() to the method signature:
        dataPlaneManager.terminate(dataFlowId)
                .orElseThrow(InvalidRequestException::new);
    }

    @POST
    @Path("/{id}/suspend")
    @Override
    public void suspend(@PathParam("id") String id, JsonObject suspendMessage) {
        throw new UnsupportedOperationException();
    }
}
