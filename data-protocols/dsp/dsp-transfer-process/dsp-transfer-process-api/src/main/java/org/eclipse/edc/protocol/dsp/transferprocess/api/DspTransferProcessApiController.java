/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.compact;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;


@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/transfers")
public class DspTransferProcessApiController {

    private Monitor monitor;

    private JsonLdTransformerRegistry registry;

    private TransferProcessService transferProcessService;

    private ObjectMapper mapper;

    public DspTransferProcessApiController(Monitor monitor, TypeManager typeManager, JsonLdTransformerRegistry registry, TransferProcessService transferProcessService) {
        this.monitor = monitor;
        this.transferProcessService = transferProcessService;
        this.registry = registry;
        this.mapper = typeManager.getMapper("json-ld");
    }

    //Provider side
    @GET
    @Path("/{id}")
    public JsonObject getTransferProcess(@PathParam("id") String id) {
        monitor.debug(format("DSP: Incoming request for transfer process with id %s", id));

        var transferProcess = transferProcessService.findById(id);

        if (transferProcess == null) {
            throw new ObjectNotFoundException(TransferProcess.class, id);
        }

        var result = registry.transform(transferProcess, JsonObject.class);

        if (result.failed()) {
            throw new EdcException("Response could not be created");
        }

        return mapper.convertValue(compact(result.getContent(), jsonLdContext()), JsonObject.class);
    }

    //Provider side
    @POST
    @Path("/request")
    public JsonObject initiateTransferProcess(JsonObject jsonObject) {
        monitor.debug("DSP: Incoming TransferRequestMessage for initiating a transfer process");

        var transferRequestMessageResult = registry.transform(expand(jsonObject).getJsonObject(0), TransferRequestMessage.class);

        if (transferRequestMessageResult.failed()) {
            throw new EdcException("Failed to create request body for transfer request message");
        }

        var transferRequestMessage = transferRequestMessageResult.getContent();

        var transferRequest = createTransferRequest(transferRequestMessage);

        var value = transferProcessService.initiateTransfer(transferRequest);

        if (value.failed()) {
            throw new EdcException("TransferProcess could not be initiated");
        }

        var transferprocess = transferProcessService.findById(value.getContent());

        var result = registry.transform(transferprocess, JsonObject.class);

        if (result.failed()) {
            throw new EdcException("Response could not be created");
        }
        return mapper.convertValue(compact(result.getContent(), jsonLdContext()), JsonObject.class);
    }

    //both sides
    @POST
    @Path("/{id}/start")
    public void consumerTransferProcessStart(@PathParam("id") String id, JsonObject jsonObject) {
        monitor.debug(format("DSP: Incoming TransferStartMessage for transfer process %s", id));

        //TODO Add Start transferProcess method in Service
    }

    //both sides
    @POST
    @Path("/{id}/completion")
    public void consumerTransferProcessCompletion(@PathParam("id") String id, JsonObject jsonObject) {
        monitor.debug(format("DSP: Incoming TransferCompletionMessage for transfer process %s", id));

        transferProcessService.complete(id);
    }

    //both sides
    @POST
    @Path("/{id}/termination")
    public void consumerTransferProcessTermination(@PathParam("id") String id, JsonObject jsonObject) {
        monitor.debug(format("DSP: Incoming TransferTerminationMessage for transfer process %s", id));

        transferProcessService.terminate(id, "API-Call");
    }

    //both sides
    @POST
    @Path("/{id}/suspension")
    public void consumerTransferProcessSuspension(@PathParam("id") String id, JsonObject jsonObject) {
        monitor.debug(format("DSP: Incoming TransferSuspensionMessage for transfer process %s", id));

        //TODO Add Suspension transferProcess method in Service
    }

    private TransferRequest createTransferRequest(TransferRequestMessage transferRequestMessage) {
        var dataRequest = DataRequest.Builder.newInstance()
                .id(transferRequestMessage.getId())
                .protocol(transferRequestMessage.getProtocol())
                .connectorAddress(transferRequestMessage.getConnectorAddress())
                .contractId(transferRequestMessage.getContractId())
                .assetId(transferRequestMessage.getAssetId())
                .properties(transferRequestMessage.getProperties())
                .connectorId(transferRequestMessage.getConnectorId());

        if (!transferRequestMessage.getDataDestination().getType().isEmpty()) {
            dataRequest.destinationType(transferRequestMessage.getDataDestination().getType());
        }

        if (transferRequestMessage.getDataDestination() != null) {
            var dataDestination = DataAddress.Builder.newInstance()
                    .properties(transferRequestMessage.getProperties())
                    .build();
            dataRequest.dataDestination(dataDestination);
        }

        return TransferRequest.Builder.newInstance()
                .dataRequest(dataRequest.build())
                .build(); //TODO Check if Callback Address is needed
    }

    private JsonObject jsonLdContext() {
        return Json.createObjectBuilder()
                //TODO ADD CONTEXTFIELDS
                .build();
    }
}