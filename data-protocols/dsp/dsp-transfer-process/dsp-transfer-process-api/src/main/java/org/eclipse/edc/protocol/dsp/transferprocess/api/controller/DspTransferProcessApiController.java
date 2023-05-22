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

package org.eclipse.edc.protocol.dsp.transferprocess.api.controller;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRemoteMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.DspError;
import org.eclipse.edc.service.spi.result.ServiceFailure;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.TypeUtil.isOfExpectedType;
import static org.eclipse.edc.protocol.dsp.DspErrorDetails.NOT_IMPLEMENTED;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_COMPLETION;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_INITIAL_REQUEST;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_START;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_SUSPENSION;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_TERMINATION;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_ERROR;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE;

/**
 * Provides the endpoints for receiving messages regarding transfers, like initiating, completing
 * and terminating a transfer process.
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path(BASE_PATH)
public class DspTransferProcessApiController {

    private final Monitor monitor;
    private final TypeTransformerRegistry registry;
    private final TransferProcessProtocolService protocolService;
    private final IdentityService identityService;
    private final String dspCallbackAddress;
    private final JsonLd jsonLdService;

    public DspTransferProcessApiController(Monitor monitor,
                                           TypeTransformerRegistry registry,
                                           TransferProcessProtocolService protocolService,
                                           IdentityService identityService,
                                           String dspCallbackAddress,
                                           JsonLd jsonLdService) {
        this.monitor = monitor;
        this.protocolService = protocolService;
        this.registry = registry;
        this.identityService = identityService;
        this.dspCallbackAddress = dspCallbackAddress;
        this.jsonLdService = jsonLdService;
    }

    /**
     * Retrieves an existing transfer process. This functionality is not yet supported.
     *
     * @param id the ID of the process
     * @return the requested transfer process or an error.
     */
    @GET
    @Path("/{id}")
    public Response getTransferProcess(@PathParam("id") String id) {
        return errorResponse(Optional.of(id), Response.Status.NOT_IMPLEMENTED, NOT_IMPLEMENTED);
    }

    /**
     * Initiates a new transfer process that has been requested by the counter-party.
     *
     * @param jsonObject the {@link TransferRequestMessage} in JSON-LD expanded form
     * @param token      the authorization header
     * @return the created transfer process or an error.
     */
    @POST
    @Path(TRANSFER_INITIAL_REQUEST)
    public Response initiateTransferProcess(JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        var transferProcessResult = handleMessage(MessageSpec.Builder.newInstance(TransferRequestMessage.class)
                .expectedMessageType(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyRequested)
                .build());

        if (transferProcessResult.failed()) {
            return errorResponse(Optional.empty(), getHttpStatus(transferProcessResult.reason()), transferProcessResult.getFailureDetail());
        }

        return registry.transform(transferProcessResult.getContent(), JsonObject.class)
                .map(transformedJson -> Response.ok().type(MediaType.APPLICATION_JSON).entity(transformedJson).build())
                .orElse(failure -> {
                    var errorCode = UUID.randomUUID();
                    monitor.warning(String.format("Error transforming transfer process, error id %s: %s", errorCode, failure.getFailureDetail()));
                    return errorResponse(Optional.of(transferProcessResult.getContent().getCorrelationId()), Response.Status.INTERNAL_SERVER_ERROR, String.format("Error code %s", errorCode));
                });
    }

    /**
     * Notifies the connector that a transfer process has been started by the counter-part.
     *
     * @param id         the ID of the process
     * @param jsonObject the {@link TransferStartMessage} in JSON-LD expanded form
     * @param token      the authorization header
     * @return empty response or error.
     */
    @POST
    @Path("{id}" + TRANSFER_START)
    public Response transferProcessStart(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        return handleMessage(MessageSpec.Builder.newInstance(TransferStartMessage.class)
                .processId(id)
                .expectedMessageType(DSPACE_TYPE_TRANSFER_START_MESSAGE)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyStarted)
                .build())
                .map(tp -> Response.ok().build())
                .orElse(createErrorResponse(id));
    }

    /**
     * Notifies the connector that a transfer process has been completed by the counter-part.
     *
     * @param id         the ID of the process
     * @param jsonObject the {@link TransferCompletionMessage} in JSON-LD expanded form
     * @param token      the authorization header
     * @return empty response or error.
     */
    @POST
    @Path("{id}" + TRANSFER_COMPLETION)
    public Response transferProcessCompletion(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        return handleMessage(MessageSpec.Builder.newInstance(TransferCompletionMessage.class)
                .processId(id)
                .expectedMessageType(DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyCompleted)
                .build())
                .map(tp -> Response.ok().build())
                .orElse(createErrorResponse(id));
    }

    /**
     * Notifies the connector that a transfer process has been terminated by the counter-part.
     *
     * @param id         the ID of the process
     * @param jsonObject the {@link TransferTerminationMessage} in JSON-LD expanded form
     * @param token      the authorization header
     * @return empty response or error.
     */
    @POST
    @Path("{id}" + TRANSFER_TERMINATION)
    public Response transferProcessTermination(@PathParam("id") String id, JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        return handleMessage(MessageSpec.Builder.newInstance(TransferTerminationMessage.class)
                .processId(id)
                .expectedMessageType(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyTerminated)
                .build())
                .map(tp -> Response.ok().build())
                .orElse(createErrorResponse(id));
    }

    /**
     * Notifies the connector that a transfer process has been suspended by the counter-part.
     * This functionality is not yet supported.
     *
     * @param id the ID of the process
     */
    @POST
    @Path("{id}" + TRANSFER_SUSPENSION)
    public Response transferProcessSuspension(@PathParam("id") String id) {
        return errorResponse(Optional.of(id), Response.Status.NOT_IMPLEMENTED, NOT_IMPLEMENTED);
    }

    @NotNull
    private Function<ServiceFailure, Response> createErrorResponse(String id) {
        return f -> errorResponse(Optional.of(id), getHttpStatus(f.getReason()), f.getFailureDetail());
    }

    @NotNull
    private Response.Status getHttpStatus(ServiceFailure.Reason reason) {
        switch (reason) {
            case UNAUTHORIZED:
                return Response.Status.UNAUTHORIZED;
            case CONFLICT:
                return Response.Status.CONFLICT;
            default:
                return Response.Status.BAD_REQUEST;
        }

    }

    /**
     * Handles an incoming message. Validates the identity token. Then verifies that the JSON-LD
     * message has the expected type, before transforming it to a respective message class instance.
     * If the process ID was part of the request's path, verifies that the ID in the message matches
     * the one from the path. Then calls the service method with message and claim token. Will throw
     * an exception if any of the operations fail.
     *
     * @param messageSpec the message spec
     * @return the transfer process returned by the service call
     */
    private <M extends TransferRemoteMessage> ServiceResult<TransferProcess> handleMessage(MessageSpec<M> messageSpec) {
        monitor.debug(() -> format("DSP: Incoming %s for transfer process%s",
                messageSpec.getMessageClass().getSimpleName(), messageSpec.getProcessId() != null ? ": " + messageSpec.getProcessId() : ""));

        var claimToken = checkAuthToken(messageSpec.getToken());
        if (claimToken.failed()) {
            return ServiceResult.unauthorized(claimToken.getFailureMessages());
        }

        var expansion = jsonLdService.expand(messageSpec.getMessage());
        if (expansion.failed()) {
            return ServiceResult.badRequest(expansion.getFailureMessages());
        }


        if (!isOfExpectedType(expansion.getContent(), messageSpec.getExpectedMessageType())) {
            return ServiceResult.badRequest(format("Request body was not of expected type: %s", messageSpec.getExpectedMessageType()));
        }
        var ingressResult = registry.transform(expansion.getContent(), messageSpec.getMessageClass())
                .compose(m -> {
                    // set the remote protocol used
                    m.setProtocol(DATASPACE_PROTOCOL_HTTP);
                    return validateProcessId(messageSpec, m);
                });

        if (ingressResult.failed()) {
            return ServiceResult.badRequest(format("Failed to read request body: %s", ingressResult.getFailureDetail()));
        }


        return messageSpec.getServiceCall().apply(ingressResult.getContent(), claimToken.getContent());
    }

    private Result<ClaimToken> checkAuthToken(String token) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(token).build();

        return identityService.verifyJwtToken(tokenRepresentation, dspCallbackAddress);

    }

    /**
     * Ensures that a process id specified in an endpoint url matches the process id in the incoming DSP Json-Ld message.
     */
    private <M extends TransferRemoteMessage> Result<M> validateProcessId(MessageSpec<?> messageSpec, M message) {
        var expected = messageSpec.getProcessId();
        if (expected != null) {
            var actual = message.getProcessId();
            if (!expected.equals(actual)) {
                return Result.failure(format("Invalid process ID. Expected: %s, actual: %s", expected, actual));
            }
        }
        return Result.success(message);
    }

    private Response errorResponse(Optional<String> processId, Response.Status code, String message) {
        var builder = DspError.Builder.newInstance()
                .type(DSPACE_TYPE_TRANSFER_ERROR)
                .code(Integer.toString(code.getStatusCode()))
                .messages(List.of(message));

        processId.ifPresent(builder::processId);

        return Response.status(code).type(MediaType.APPLICATION_JSON)
                .entity(builder.build().toJson())
                .build();
    }

}
