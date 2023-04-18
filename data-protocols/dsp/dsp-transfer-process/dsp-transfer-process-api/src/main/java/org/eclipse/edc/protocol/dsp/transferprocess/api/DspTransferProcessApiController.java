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
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.transferprocess.spi.type.TransferError;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.compact;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCT_SCHEMA;


@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/transfers")
public class DspTransferProcessApiController {

    private Monitor monitor;

    private JsonLdTransformerRegistry registry;

    private TransferProcessService transferProcessService;

    private ObjectMapper mapper;

    private IdentityService identityService;

    private String dspCallbackAddress;

    public DspTransferProcessApiController(Monitor monitor, TypeManager typeManager, JsonLdTransformerRegistry registry, TransferProcessService transferProcessService,
                                           IdentityService identityService, String dspCallbackAddress) {
        this.monitor = monitor;
        this.transferProcessService = transferProcessService;
        this.registry = registry;
        this.mapper = typeManager.getMapper("json-ld");
        this.identityService = identityService;
        this.dspCallbackAddress = dspCallbackAddress;
    }

    //Provider side
    @GET
    @Path("/{id}")
    public JsonObject getTransferProcess(@PathParam("id") String id, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming request for transfer process with id %s", id));

        checkAuthToken(token);

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
    public JsonObject initiateTransferProcess(JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug("DSP: Incoming TransferRequestMessage for initiating a transfer process");

        checkAuthToken(token);

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
    public void consumerTransferProcessStart(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming TransferStartMessage for transfer process %s", id));

        checkAuthToken(token);

        //TODO Add Start transferProcess method in Service
    }

    //both sides
    @POST
    @Path("/{id}/completion")
    public void consumerTransferProcessCompletion(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming TransferCompletionMessage for transfer process %s", id));

        checkAuthToken(token);

        transferProcessService.complete(id);
    }

    //both sides
    @POST
    @Path("/{id}/termination")
    public void consumerTransferProcessTermination(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming TransferTerminationMessage for transfer process %s", id));

        checkAuthToken(token);

        transferProcessService.terminate(id, "API-Call");
    }

    //both sides
    @POST
    @Path("/{id}/suspension")
    public void consumerTransferProcessSuspension(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming TransferSuspensionMessage for transfer process %s", id));

        checkAuthToken(token);

        //TODO Add Suspension transferProcess method in Service
    }

    private TransferRequest createTransferRequest(TransferRequestMessage transferRequestMessage) {
        var dataRequest = DataRequest.Builder.newInstance()
                .id(transferRequestMessage.getId())
                .protocol(transferRequestMessage.getProtocol())
                .connectorAddress(transferRequestMessage.getConnectorAddress())
                .contractId(transferRequestMessage.getContractId())
                .dataDestination(transferRequestMessage.getDataDestination());


        return TransferRequest.Builder.newInstance()
                .dataRequest(dataRequest.build())
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance().events(Set.of()).uri(transferRequestMessage.getConnectorAddress()).build()))
                .build(); //TODO Events for Callback Address is needed
    }

    private void checkAuthToken(String token) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();

        var result = identityService.verifyJwtToken(tokenRepresentation, dspCallbackAddress);
        if (result.failed()) {
            throw new AuthenticationFailedException();
        }
    }

    private JsonObject jsonLdContext() {
        return Json.createObjectBuilder()
                .add(DCT_PREFIX, DCT_SCHEMA)
                .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                .build();
    }

    private String createTransferProcessError(String code, String processId, String correlationId, String reason) {
        var error = TransferError.Builder.newInstance()
                .code(code)
                .processId(processId)
                .reason(List.of(reason));
        if (correlationId != null) {
            error.correlationId(correlationId);
        }

        var result = registry.transform(error.build(), JsonObject.class);

        return mapper.convertValue(compact(result.getContent(), jsonLdContext()), JsonObject.class).toString();
    }
}