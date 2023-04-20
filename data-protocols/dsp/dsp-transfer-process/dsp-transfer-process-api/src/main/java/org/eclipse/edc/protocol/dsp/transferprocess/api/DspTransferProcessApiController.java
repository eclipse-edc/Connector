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
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

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

/**
 * Provides the endpoints for receiving messages regarding transfers, like initiating, completing
 * and terminating a transfer process.
 */
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
    
    /**
     * Retrieves an existing transfer process. This functionality is not yet supported.
     *
     * @param id the ID of the process
     * @return the transfer process in JSON-LD expanded form
     */
    @GET
    @Path("/{id}")
    public JsonObject getTransferProcess(@PathParam("id") String id) {
        monitor.debug(format("DSP: Incoming request for transfer process with id %s", id));
    
        throw new UnsupportedOperationException("Getting a transfer process not yet supported.");
    }
    
    /**
     * Initiates a new transfer process that has been requested by the counter-party.
     *
     * @param jsonObject the {@link TransferRequestMessage} in JSON-LD expanded form
     * @param token the authorization header
     * @return the created transfer process  in JSON-LD expanded form
     */
    @POST
    @Path(TRANSFER_INITIAL_REQUEST)
    public Map<String, Object> initiateTransferProcess(JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug("DSP: Incoming TransferRequestMessage for initiating a transfer process");

        var claimToken = checkAuthToken(token);

        var messageResult = registry.transform(expand(jsonObject).getJsonObject(0), TransferRequestMessage.class);
        if (messageResult.failed()) {
            throw new InvalidRequestException(format("Failed to read request body: %s", join(", ", messageResult.getFailureMessages())));
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
    
    /**
     * Notifies the connector that a transfer process has been started by the counter-part.
     *
     * @param id the ID of the process
     * @param jsonObject the {@link TransferStartMessage} in JSON-LD expanded form
     * @param token the authorization header
     */
    @POST
    @Path("{id}" + TRANSFER_START)
    public void transferProcessStart(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming TransferStartMessage for transfer process %s", id));

        var claimToken = checkAuthToken(token);
    
        var result = registry.transform(expand(jsonObject).getJsonObject(0), TransferStartMessage.class);
        if (result.failed()) {
            throw new InvalidRequestException(format("Failed to read request body: %s", join(", ", result.getFailureMessages())));
        }

        var serviceResult = protocolService.notifyStarted(result.getContent(), claimToken);
        if (serviceResult.failed()) {
            throw new EdcException(format("Failed to start transfer: %s", join(", ", serviceResult.getFailureMessages())));
        }
    }
    
    /**
     * Notifies the connector that a transfer process has been completed by the counter-part.
     *
     * @param id the ID of the process
     * @param jsonObject the {@link TransferCompletionMessage} in JSON-LD expanded form
     * @param token the authorization header
     */
    @POST
    @Path("{id}" + TRANSFER_COMPLETION)
    public void transferProcessCompletion(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming TransferCompletionMessage for transfer process %s", id));
    
        var claimToken = checkAuthToken(token);
    
        var result = registry.transform(expand(jsonObject).getJsonObject(0), TransferCompletionMessage.class);
        if (result.failed()) {
            throw new InvalidRequestException(format("Failed to read request body: %s", join(", ", result.getFailureMessages())));
        }
    
        var serviceResult = protocolService.notifyCompleted(result.getContent(), claimToken);
        if (serviceResult.failed()) {
            throw new EdcException(format("Failed to complete transfer: %s", join(", ", serviceResult.getFailureMessages())));
        }
    }
    
    /**
     * Notifies the connector that a transfer process has been terminated by the counter-part.
     *
     * @param id the ID of the process
     * @param jsonObject the {@link TransferTerminationMessage} in JSON-LD expanded form
     * @param token the authorization header
     */
    @POST
    @Path("{id}" + TRANSFER_TERMINATION)
    public void transferProcessTermination(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming TransferTerminationMessage for transfer process %s", id));
    
        var claimToken = checkAuthToken(token);
    
        var result = registry.transform(expand(jsonObject).getJsonObject(0), TransferTerminationMessage.class);
        if (result.failed()) {
            throw new InvalidRequestException(format("Failed to read request body: %s", join(", ", result.getFailureMessages())));
        }
    
        var serviceResult = protocolService.notifyTerminated(result.getContent(), claimToken);
        if (serviceResult.failed()) {
            throw new EdcException(format("Failed to terminate transfer: %s", join(", ", serviceResult.getFailureMessages())));
        }
    }
    
    /**
     * Notifies the connector that a transfer process has been suspended by the counter-part.
     * This functionality is not yet supported.
     *
     * @param id the ID of the process
     */
    @POST
    @Path("{id}" + TRANSFER_SUSPENSION)
    public void transferProcessSuspension(@PathParam("id") String id) {
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