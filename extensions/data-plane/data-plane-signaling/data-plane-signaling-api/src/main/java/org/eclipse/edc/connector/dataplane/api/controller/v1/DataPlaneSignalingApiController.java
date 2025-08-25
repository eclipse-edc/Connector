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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/v1/dataflows")
public class DataPlaneSignalingApiController implements DataPlaneSignalingApi {

    private final TypeTransformerRegistry typeTransformerRegistry;
    private final DataPlaneManager dataPlaneManager;
    private final Monitor monitor;

    public DataPlaneSignalingApiController(TypeTransformerRegistry typeTransformerRegistry, DataPlaneManager dataPlaneManager, Monitor monitor) {
        this.typeTransformerRegistry = typeTransformerRegistry;
        this.dataPlaneManager = dataPlaneManager;
        this.monitor = monitor;
    }

    @POST
    @Override
    public JsonObject start(JsonObject message) {
        var type = message.getJsonArray(TYPE).getString(0);

        var response = switch (type) {
            case DataFlowStartMessage.EDC_DATA_FLOW_START_MESSAGE_TYPE -> startFlow(message);
            case DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_TYPE -> provisionFlow(message);
            default -> throw new InvalidRequestException("Type " + type + " not valid");
        };

        return typeTransformerRegistry.transform(response, JsonObject.class)
                    .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    @GET
    @Path("/{id}/state")
    @Override
    public JsonObject getTransferState(@PathParam("id") String transferProcessId) {
        var state = dataPlaneManager.getTransferState(transferProcessId);
        // not really worth to create a dedicated transformer for this simple object

        return Json.createObjectBuilder()
                .add(TYPE, "DataFlowState")
                .add(EDC_NAMESPACE + "state", state.toString())
                .build();
    }

    @POST
    @Path("/{id}/terminate")
    @Override
    public void terminate(@PathParam("id") String dataFlowId, JsonObject terminationMessage) {

        var msg = typeTransformerRegistry.transform(terminationMessage, DataFlowTerminateMessage.class)
                .onFailure(f -> monitor.warning("Error transforming %s: %s".formatted(DataFlowTerminateMessage.class, f.getFailureDetail())))
                .orElseThrow(InvalidRequestException::new);

        dataPlaneManager.terminate(dataFlowId, msg.getReason()).orElseThrow(InvalidRequestException::new);
    }

    @POST
    @Path("/{id}/suspend")
    @Override
    public void suspend(@PathParam("id") String id, JsonObject suspendMessage) {
        terminate(id, suspendMessage);
    }

    @Override
    @Path("/check")
    @GET
    public void checkAvailability() {

    }

    private DataFlowResponseMessage provisionFlow(JsonObject message) {
        var provisionMsg = typeTransformerRegistry.transform(message, DataFlowProvisionMessage.class)
                .onFailure(f -> monitor.warning("Error transforming %s: %s".formatted(DataFlowProvisionMessage.class, f.getFailureDetail())))
                .orElseThrow(InvalidRequestException::new);

        return dataPlaneManager.provision(provisionMsg)
                .orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));
    }

    private DataFlowResponseMessage startFlow(JsonObject message) {
        var startMessage = typeTransformerRegistry.transform(message, DataFlowStartMessage.class)
                .onFailure(f -> monitor.warning("Error transforming %s: %s".formatted(DataFlowStartMessage.class, f.getFailureDetail())))
                .orElseThrow(InvalidRequestException::new);

        dataPlaneManager.validate(startMessage)
                .onFailure(f -> monitor.warning("Failed to validate request: %s".formatted(f.getFailureDetail())))
                .orElseThrow(f -> f.getMessages().isEmpty() ?
                        new InvalidRequestException("Failed to validate request: %s".formatted(startMessage.getId())) :
                        new InvalidRequestException(f.getMessages()));

        return dataPlaneManager.start(startMessage)
                .orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));
    }

}
