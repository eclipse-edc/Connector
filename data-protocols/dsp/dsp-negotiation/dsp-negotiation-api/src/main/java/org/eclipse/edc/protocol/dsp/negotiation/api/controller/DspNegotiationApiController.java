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

package org.eclipse.edc.protocol.dsp.negotiation.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.negotiation.transform.ContractNegotiationError;
import org.eclipse.edc.protocol.dsp.spi.mapper.DspHttpStatusCodeMapper;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.TypeUtil.isOfExpectedType;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.CONTRACT_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.EVENT;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.TERMINATION;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.VERIFICATION;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_AGREEMENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_CONTRACT_OFFER_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_EVENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_TERMINATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

/**
 * Provides consumer and provider endpoints for the contract negotiation according to the http binding
 * of the dataspace protocol.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path(BASE_PATH)
public class DspNegotiationApiController {

    private final IdentityService identityService;
    private final TypeTransformerRegistry transformerRegistry;
    private final String callbackAddress;
    private final Monitor monitor;
    private final ContractNegotiationProtocolService protocolService;
    private final ObjectMapper mapper;
    private final JsonLd jsonLdService;
    private final DspHttpStatusCodeMapper statusCodeMapper;

    public DspNegotiationApiController(String callbackAddress,
                                       IdentityService identityService,
                                       TypeTransformerRegistry transformerRegistry,
                                       ContractNegotiationProtocolService protocolService,
                                       JsonLd jsonLdService,
                                       ObjectMapper mapper,
                                       Monitor monitor,
                                       DspHttpStatusCodeMapper statusCodeMapper) {
        this.callbackAddress = callbackAddress;
        this.identityService = identityService;
        this.monitor = monitor;
        this.protocolService = protocolService;
        this.transformerRegistry = transformerRegistry;
        this.mapper = mapper;
        this.jsonLdService = jsonLdService;
        this.statusCodeMapper = statusCodeMapper;
    }

    /**
     * Provider-specific endpoint.
     *
     * @param id    of contract negotiation.
     * @param token identity token.
     * @return {@link Response} ErrorResponse
     */
    @GET
    @Path("{id}")
    public Response getNegotiation(@PathParam("id") String id, @HeaderParam(AUTHORIZATION) String token) {
        try {
            checkAndReturnAuthToken(token);
            throw new UnsupportedOperationException("Currently not supported.");
        } catch (Exception exception) {
            var entity = transformerRegistry.transform(new ContractNegotiationError(Optional.empty(), exception), JsonObject.class).getContent();

            return createResponse(entity, exception);
        }
    }

    /**
     * Provider-specific endpoint.
     *
     * @param jsonObject dspace:ContractRequestMessage sent by a consumer.
     * @param token      identity token.
     * @return {@link Response} ContractNegotiation or ErrorResponse
     */
    @POST
    @Path(INITIAL_CONTRACT_REQUEST)
    public Response initiateNegotiation(@RequestBody(description = DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE, required = true)
                                        JsonObject jsonObject,
                                        @HeaderParam(AUTHORIZATION) String token) {
        try {
            var negotiation = handleMessage(MessageSpec.Builder.newInstance(ContractRequestMessage.class)
                    .expectedMessageType(DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE)
                    .message(jsonObject)
                    .token(token)
                    .serviceCall(this::validateAndProcessRequest)
                    .build());

            var result = transformerRegistry.transform(negotiation, JsonObject.class)
                    .orElseThrow(failure -> new EdcException(format("Failed to build response: %s", failure.getFailureDetail())));

            var compactResult = jsonLdService.compact(result);

            //noinspection unchecked
            var entity = compactResult.map(jo -> mapper.convertValue(jo, Map.class)).orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));
            return Response.status(Response.Status.OK).entity(entity).build();
        } catch (Exception exception) {
            var entity = transformerRegistry.transform(new ContractNegotiationError(Optional.empty(), exception), JsonObject.class).getContent();

            return createResponse(entity, exception);
        }
    }

    /**
     * Provider-specific endpoint.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractRequestMessage sent by a consumer.
     * @param token      identity token.
     * @return {@link Response} Empty Success Response or ErrorResponse
     */
    @POST
    @Path("{id}" + CONTRACT_REQUEST)
    public Response consumerOffer(@PathParam("id") String id,
                                  @RequestBody(description = DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE, required = true)
                                  JsonObject jsonObject,
                                  @HeaderParam(AUTHORIZATION) String token) {
        try {
            handleMessage(MessageSpec.Builder.newInstance(ContractRequestMessage.class)
                    .expectedMessageType(DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE)
                    .processId(id)
                    .message(jsonObject)
                    .token(token)
                    .serviceCall(protocolService::notifyRequested)
                    .build());

            return Response.status(Response.Status.OK).build();
        } catch (Exception exception) {
            var entity = transformerRegistry.transform(new ContractNegotiationError(Optional.of(id), exception), JsonObject.class).getContent();

            return createResponse(entity, exception);
        }
    }

    /**
     * Endpoint on provider and consumer side.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractNegotiationEventMessage sent by consumer or provider.
     * @param token      identity token.
     * @return {@link Response} Empty Success Response or ErrorResponse
     */
    @POST
    @Path("{id}" + EVENT)
    public Response createEvent(@PathParam("id") String id,
                                @RequestBody(description = DSPACE_NEGOTIATION_EVENT_MESSAGE, required = true)
                                JsonObject jsonObject,
                                @HeaderParam(AUTHORIZATION) String token) {
        try {
            handleMessage(MessageSpec.Builder.newInstance(ContractNegotiationEventMessage.class)
                    .expectedMessageType(DSPACE_NEGOTIATION_EVENT_MESSAGE)
                    .processId(id)
                    .message(jsonObject)
                    .token(token)
                    .serviceCall(this::dispatchEventMessage)
                    .build());

            return Response.status(Response.Status.OK).build();
        } catch (Exception exception) {
            var entity = transformerRegistry.transform(new ContractNegotiationError(Optional.of(id), exception), JsonObject.class).getContent();

            return createResponse(entity, exception);
        }
    }

    /**
     * Provider-specific endpoint.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractAgreementVerificationMessage sent by a consumer.
     * @param token      identity token.
     * @return {@link Response} Empty Success Response or ErrorResponse
     */
    @POST
    @Path("{id}" + AGREEMENT + VERIFICATION)
    public Response verifyAgreement(@PathParam("id") String id,
                                    @RequestBody(description = DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE, required = true)
                                    JsonObject jsonObject,
                                    @HeaderParam(AUTHORIZATION) String token) {
        try {
            handleMessage(MessageSpec.Builder.newInstance(ContractAgreementVerificationMessage.class)
                    .expectedMessageType(DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE)
                    .processId(id)
                    .message(jsonObject)
                    .token(token)
                    .serviceCall(protocolService::notifyVerified)
                    .build());

            return Response.status(Response.Status.OK).build();
        } catch (Exception exception) {
            var entity = transformerRegistry.transform(new ContractNegotiationError(Optional.of(id), exception), JsonObject.class).getContent();

            return createResponse(entity, exception);
        }

    }

    /**
     * Endpoint on provider and consumer side.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractNegotiationTerminationMessage sent by consumer or provider.
     * @param token      identity token.
     * @return {@link Response} Empty Success Response or ErrorResponse
     */
    @POST
    @Path("{id}" + TERMINATION)
    public Response terminateNegotiation(@PathParam("id") String id,
                                     @RequestBody(description = DSPACE_NEGOTIATION_TERMINATION_MESSAGE, required = true)
                                     JsonObject jsonObject,
                                     @HeaderParam(AUTHORIZATION) String token) {
        try {
            handleMessage(MessageSpec.Builder.newInstance(ContractNegotiationTerminationMessage.class)
                    .expectedMessageType(DSPACE_NEGOTIATION_TERMINATION_MESSAGE)
                    .processId(id)
                    .message(jsonObject)
                    .token(token)
                    .serviceCall(protocolService::notifyTerminated)
                    .build());

            return Response.status(Response.Status.OK).build();
        } catch (Exception exception) {
            var entity = transformerRegistry.transform(new ContractNegotiationError(Optional.of(id), exception), JsonObject.class).getContent();

            return createResponse(entity, exception);
        }
    }

    /**
     * Consumer-specific endpoint.
     *
     * @param id    of contract negotiation.
     * @param body  dspace:ContractOfferMessage sent by a provider.
     * @param token identity token.
     * @return {@link Response} ErrorResponse
     */
    @POST
    @Path("{id}" + CONTRACT_OFFER)
    public Response providerOffer(@PathParam("id") String id,
                              @RequestBody(description = DSPACE_NEGOTIATION_CONTRACT_OFFER_MESSAGE, required = true)
                              JsonObject body,
                              @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(() -> format("DSP: Incoming ContractOfferMessage for process %s", id));

        try {
            checkAndReturnAuthToken(token);

            throw new UnsupportedOperationException("Currently not supported.");
        } catch (Exception exception) {
            var entity = transformerRegistry.transform(new ContractNegotiationError(Optional.of(id), exception), JsonObject.class).getContent();

            return createResponse(entity, exception);
        }
    }

    /**
     * Consumer-specific endpoint.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractAgreementMessage sent by a provider.
     * @param token      identity token.
     * @return {@link Response} Empty Response or ErrorResponse
     */
    @POST
    @Path("{id}" + AGREEMENT)
    public Response createAgreement(@PathParam("id") String id,
                                @RequestBody(description = DSPACE_NEGOTIATION_AGREEMENT_MESSAGE, required = true)
                                JsonObject jsonObject,
                                @HeaderParam(AUTHORIZATION) String token) {
        try {
            handleMessage(MessageSpec.Builder.newInstance(ContractAgreementMessage.class)
                    .expectedMessageType(DSPACE_NEGOTIATION_AGREEMENT_MESSAGE)
                    .processId(id)
                    .message(jsonObject)
                    .token(token)
                    .serviceCall(protocolService::notifyAgreed)
                    .build());
            return Response.status(Response.Status.OK).build();
        } catch (Exception exception) {
            var entity = transformerRegistry.transform(new ContractNegotiationError(Optional.of(id), exception), JsonObject.class).getContent();

            return createResponse(entity, exception);
        }
    }

    private <M extends ContractRemoteMessage> ContractNegotiation handleMessage(MessageSpec<M> messageSpec) {
        monitor.debug(() -> format("DSP: Incoming %s for contract negotiation process%s",
                messageSpec.getMessageClass().getSimpleName(), messageSpec.getProcessId() != null ? ": " + messageSpec.getProcessId() : ""));

        var claimToken = checkAndReturnAuthToken(messageSpec.getToken());
        var expanded = jsonLdService.expand(messageSpec.getMessage())
                .map(ej -> ej).orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));

        validateType(expanded, messageSpec.getExpectedMessageType());

        var message = transformerRegistry.transform(expanded, messageSpec.getMessageClass())
                .orElseThrow(failure -> new InvalidRequestException(format("Request body was malformed: %s", failure.getFailureDetail())));

        // set the remote protocol used
        message.setProtocol(DATASPACE_PROTOCOL_HTTP);

        validateProcessId(messageSpec.getProcessId(), message.getProcessId());

        // invokes negotiation protocol service method
        return messageSpec.getServiceCall().apply(message, claimToken).orElseThrow(exceptionMapper(ContractNegotiation.class));
    }

    @NotNull
    private ServiceResult<ContractNegotiation> validateAndProcessRequest(ContractRequestMessage message, ClaimToken claimToken) {
        if (message.getCallbackAddress() == null) {
            throw new InvalidRequestException(format("ContractRequestMessage must contain a '%s' property", DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS));
        }
        return protocolService.notifyRequested(message, claimToken);
    }

    @NotNull
    private ServiceResult<ContractNegotiation> dispatchEventMessage(ContractNegotiationEventMessage message, ClaimToken claimToken) {
        switch (message.getType()) {
            case FINALIZED:
                return protocolService.notifyFinalized(message, claimToken);
            case ACCEPTED:
                return protocolService.notifyAccepted(message, claimToken);
            default:
                throw new InvalidRequestException(String.format("Cannot process dspace:ContractNegotiationEventMessage with unexpected type %s.", message.getType()));
        }
    }

    private ClaimToken checkAndReturnAuthToken(String token) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(token).build();
        return identityService.verifyJwtToken(tokenRepresentation, callbackAddress).orElseThrow(failure ->
                new AuthenticationFailedException(format("Authentication failed: %s", failure.getFailureDetail())));
    }

    private void validateType(JsonObject object, String expected) {
        if (!isOfExpectedType(object, expected)) {
            throw new InvalidRequestException(format("Request body was not of expected type: %s", expected));
        }
    }

    private void validateProcessId(@Nullable String expected, String actual) {
        if (expected == null) {
            return;
        }
        if (!expected.equals(actual)) {
            throw new InvalidRequestException(format("Invalid process ID. Expected: %s, actual: %s", expected, actual));
        }
    }

    private Response createResponse(JsonObject jsonEntity, Exception exception) {
        JsonObject compacted = null;
        try {
            compacted = jsonLdService.compact(jsonEntity)
                    .orElseThrow(failure -> new EdcException("Failed to compact JSON-LD."));
            return Response.status(statusCodeMapper.mapErrorToStatusCode(exception)).entity(compacted).build();
        } catch (EdcException e) {
            return Response.status(500).entity("{\"message\": \"Failed to create response\"").build();
        }
    }
}
