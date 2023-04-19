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
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;

import java.util.Map;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.eclipse.edc.jsonld.JsonLdExtension.TYPE_MANAGER_CONTEXT_JSON_LD;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.compact;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.TRANSFER_COMPLETION;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.TRANSFER_INITIAL_REQUEST;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.TRANSFER_START;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.TRANSFER_SUSPENSION;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.TRANSFER_TERMINATION;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCT_SCHEMA;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path(BASE_PATH)
public class DspTransferProcessApiController {

    private Monitor monitor;
    private JsonLdTransformerRegistry registry;
    private TransferProcessProtocolService protocolService;
    private ObjectMapper mapper;
    private IdentityService identityService;
    private String dspCallbackAddress;

    public DspTransferProcessApiController(Monitor monitor, TypeManager typeManager, JsonLdTransformerRegistry registry,
                                           TransferProcessProtocolService protocolService, IdentityService identityService, String dspCallbackAddress) {
        this.monitor = monitor;
        this.protocolService = protocolService;
        this.registry = registry;
        this.mapper = typeManager.getMapper(TYPE_MANAGER_CONTEXT_JSON_LD);
        this.identityService = identityService;
        this.dspCallbackAddress = dspCallbackAddress;
    }

    //Provider side
    @GET
    @Path("/{id}")
    public JsonObject getTransferProcess(@PathParam("id") String id, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming request for transfer process with id %s", id));
    
        throw new UnsupportedOperationException("Getting a transfer process not yet supported.");
    }

    //Provider side
    @POST
    @Path(TRANSFER_INITIAL_REQUEST)
    public Map<String, Object> initiateTransferProcess(JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug("DSP: Incoming TransferRequestMessage for initiating a transfer process");

        var claimToken = checkAuthToken(token);

        var messageResult = registry.transform(expand(jsonObject).getJsonObject(0), TransferRequestMessage.class);
        if (messageResult.failed()) {
            throw new EdcException("Failed to create request body for transfer request message");
        }

        var requestMessage = messageResult.getContent();
        var initiateResult = protocolService.notifyRequested(requestMessage, claimToken);
        if (initiateResult.failed()) {
            throw new EdcException("TransferProcess could not be initiated");
        }

        var transferprocess = initiateResult.getContent();
        var transferProcessResult = registry.transform(transferprocess, JsonObject.class);
        if (transferProcessResult.failed()) {
            throw new EdcException("Response could not be created");
        }
        
        return mapper.convertValue(compact(transferProcessResult.getContent(), jsonLdContext()), Map.class);
    }

    //both sides
    @POST
    @Path("{id}" + TRANSFER_START)
    public void transferProcessStart(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming TransferStartMessage for transfer process %s", id));

        var claimToken = checkAuthToken(token);
    
        var result = registry.transform(expand(jsonObject).getJsonObject(0), TransferStartMessage.class);
        if (result.failed()) {
            throw new EdcException("Failed to create request body for transfer start message");
        }

        var serviceResult = protocolService.notifyStarted(result.getContent(), claimToken);
        if (serviceResult.failed()) {
            throw new EdcException(format("Failed to start transfer: %s", join(", ", serviceResult.getFailureMessages())));
        }
    }

    //both sides
    @POST
    @Path("{id}" + TRANSFER_COMPLETION)
    public void transferProcessCompletion(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming TransferCompletionMessage for transfer process %s", id));
    
        var claimToken = checkAuthToken(token);
    
        var result = registry.transform(expand(jsonObject).getJsonObject(0), TransferCompletionMessage.class);
        if (result.failed()) {
            throw new EdcException("Failed to create request body for transfer completion message");
        }
    
        var serviceResult = protocolService.notifyCompleted(result.getContent(), claimToken);
        if (serviceResult.failed()) {
            throw new EdcException(format("Failed to complete transfer: %s", join(", ", serviceResult.getFailureMessages())));
        }
    }

    //both sides
    @POST
    @Path("{id}" + TRANSFER_TERMINATION)
    public void transferProcessTermination(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming TransferTerminationMessage for transfer process %s", id));
    
        var claimToken = checkAuthToken(token);
    
        var result = registry.transform(expand(jsonObject).getJsonObject(0), TransferTerminationMessage.class);
        if (result.failed()) {
            throw new EdcException("Failed to create request body for transfer termination message");
        }
    
        var serviceResult = protocolService.notifyTerminated(result.getContent(), claimToken);
        if (serviceResult.failed()) {
            throw new EdcException(format("Failed to terminate transfer: %s", join(", ", serviceResult.getFailureMessages())));
        }
    }

    //both sides
    @POST
    @Path("{id}" + TRANSFER_SUSPENSION)
    public void transferProcessSuspension(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming TransferSuspensionMessage for transfer process %s", id));

        throw new UnsupportedOperationException("Suspension not yet supported.");
    }

    private ClaimToken checkAuthToken(String token) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();

        var result = identityService.verifyJwtToken(tokenRepresentation, dspCallbackAddress);
        if (result.failed()) {
            throw new AuthenticationFailedException();
        }
        
        return result.getContent();
    }

    private JsonObject jsonLdContext() {
        return Json.createObjectBuilder()
                .add(DCT_PREFIX, DCT_SCHEMA)
                .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                .build();
    }
}