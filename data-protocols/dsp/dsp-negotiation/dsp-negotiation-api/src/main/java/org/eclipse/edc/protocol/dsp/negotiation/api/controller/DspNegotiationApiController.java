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
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.protocol.dsp.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.spi.message.PostDspRequest;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.jetbrains.annotations.NotNull;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.CONTRACT_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.EVENT;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.TERMINATION;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.VERIFICATION;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_OFFER_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS;

/**
 * Provides consumer and provider endpoints for the contract negotiation according to the http binding
 * of the dataspace protocol.
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path(BASE_PATH)
public class DspNegotiationApiController {

    private final TypeTransformerRegistry transformerRegistry;
    private final DspRequestHandler dspRequestHandler;
    private final Monitor monitor;
    private final ContractNegotiationProtocolService protocolService;

    public DspNegotiationApiController(TypeTransformerRegistry transformerRegistry,
                                       ContractNegotiationProtocolService protocolService,
                                       Monitor monitor,
                                       DspRequestHandler dspRequestHandler) {
        this.monitor = monitor;
        this.protocolService = protocolService;
        this.transformerRegistry = transformerRegistry;
        this.dspRequestHandler = dspRequestHandler;
    }

    /**
     * Provider-specific endpoint.
     *
     * @param id    of contract negotiation.
     * @param token identity token.
     * @return the requested contract negotiation or an error.
     */
    @GET
    @Path("{id}")
    public Response getNegotiation(@PathParam("id") String id, @HeaderParam(AUTHORIZATION) String token) {
        var request = GetDspRequest.Builder.newInstance(ContractNegotiation.class)
                .id(id).token(token).serviceCall(protocolService::findById)
                .errorType(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR)
                .build();

        return dspRequestHandler.getResource(request);
    }

    /**
     * Provider-specific endpoint.
     *
     * @param jsonObject dspace:ContractRequestMessage sent by a consumer.
     * @param token      identity token.
     * @return the created contract negotiation or an error.
     */
    @POST
    @Path(INITIAL_CONTRACT_REQUEST)
    public Response initiateNegotiation(JsonObject jsonObject,
                                        @HeaderParam(AUTHORIZATION) String token) {
        var request = PostDspRequest.Builder.newInstance(ContractRequestMessage.class, ContractNegotiation.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE)
                .message(jsonObject)
                .token(token)
                .serviceCall(this::validateAndProcessRequest)
                .errorType(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR)
                .build();

        return dspRequestHandler.createResource(request);
    }

    /**
     * Provider-specific endpoint.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractRequestMessage sent by a consumer.
     * @param token      identity token.
     * @return the created contract negotiation or an error.
     */
    @POST
    @Path("{id}" + CONTRACT_REQUEST)
    public Response consumerOffer(@PathParam("id") String id,
                                  JsonObject jsonObject,
                                  @HeaderParam(AUTHORIZATION) String token) {
        var request = PostDspRequest.Builder.newInstance(ContractRequestMessage.class, ContractNegotiation.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyRequested)
                .errorType(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR)
                .build();

        return dspRequestHandler.updateResource(request);
    }

    /**
     * Endpoint on provider and consumer side.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractNegotiationEventMessage sent by consumer or provider.
     * @param token      identity token.
     * @return empty response or error.
     */
    @POST
    @Path("{id}" + EVENT)
    public Response createEvent(@PathParam("id") String id,
                                JsonObject jsonObject,
                                @HeaderParam(AUTHORIZATION) String token) {
        var request = PostDspRequest.Builder.newInstance(ContractNegotiationEventMessage.class, ContractNegotiation.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall((message, claimToken) -> switch (message.getType()) {
                    case FINALIZED -> protocolService.notifyFinalized(message, claimToken);
                    case ACCEPTED -> protocolService.notifyAccepted(message, claimToken);
                })
                .errorType(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR)
                .build();

        return dspRequestHandler.updateResource(request);
    }

    /**
     * Provider-specific endpoint.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractAgreementVerificationMessage sent by a consumer.
     * @param token      identity token.
     * @return empty response or error.
     */
    @POST
    @Path("{id}" + AGREEMENT + VERIFICATION)
    public Response verifyAgreement(@PathParam("id") String id,
                                    JsonObject jsonObject,
                                    @HeaderParam(AUTHORIZATION) String token) {
        var request = PostDspRequest.Builder.newInstance(ContractAgreementVerificationMessage.class, ContractNegotiation.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyVerified)
                .errorType(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR)
                .build();

        return dspRequestHandler.updateResource(request);
    }

    /**
     * Endpoint on provider and consumer side.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractNegotiationTerminationMessage sent by consumer or provider.
     * @param token      identity token.
     * @return empty response or error.
     */
    @POST
    @Path("{id}" + TERMINATION)
    public Response terminateNegotiation(@PathParam("id") String id,
                                         JsonObject jsonObject,
                                         @HeaderParam(AUTHORIZATION) String token) {
        var request = PostDspRequest.Builder.newInstance(ContractNegotiationTerminationMessage.class, ContractNegotiation.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyTerminated)
                .errorType(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR)
                .build();

        return dspRequestHandler.updateResource(request);
    }

    /**
     * Consumer-specific endpoint.
     *
     * @param id    of contract negotiation.
     * @param body  dspace:ContractOfferMessage sent by a provider.
     * @param token identity token.
     * @return empty response or error.
     */
    @POST
    @Path("{id}" + CONTRACT_OFFER)
    public Response providerOffer(@PathParam("id") String id,
                                  JsonObject body,
                                  @HeaderParam(AUTHORIZATION) String token) {
        var request = PostDspRequest.Builder.newInstance(ContractOfferMessage.class, ContractNegotiation.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE)
                .processId(id)
                .message(body)
                .token(token)
                .serviceCall(protocolService::notifyOffered)
                .errorType(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR)
                .build();

        return dspRequestHandler.updateResource(request);
    }

    /**
     * Consumer-specific endpoint.
     *
     * @param id         of contract negotiation.
     * @param jsonObject dspace:ContractAgreementMessage sent by a provider.
     * @param token      identity token.
     * @return empty response or error.
     */
    @POST
    @Path("{id}" + AGREEMENT)
    public Response createAgreement(@PathParam("id") String id,
                                    JsonObject jsonObject,
                                    @HeaderParam(AUTHORIZATION) String token) {
        var request = PostDspRequest.Builder.newInstance(ContractAgreementMessage.class, ContractNegotiation.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyAgreed)
                .errorType(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR)
                .build();

        return dspRequestHandler.updateResource(request);
    }

    @NotNull
    private ServiceResult<ContractNegotiation> validateAndProcessRequest(ContractRequestMessage message, ClaimToken claimToken) {
        if (message.getCallbackAddress() == null) {
            throw new InvalidRequestException(format("ContractRequestMessage must contain a '%s' property", DSPACE_PROPERTY_CALLBACK_ADDRESS));
        }
        return protocolService.notifyRequested(message, claimToken);
    }

}
