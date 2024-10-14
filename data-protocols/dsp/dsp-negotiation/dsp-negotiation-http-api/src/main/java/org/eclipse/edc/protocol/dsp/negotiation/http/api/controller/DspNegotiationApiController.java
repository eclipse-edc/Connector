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

package org.eclipse.edc.protocol.dsp.negotiation.http.api.controller;

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
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationError;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.http.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.PostDspRequest;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.CONTRACT_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.EVENT;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.INITIAL_CONTRACT_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.TERMINATION;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.VERIFICATION;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_OFFER_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE;

/**
 * Provides consumer and provider endpoints for the contract negotiation according to the http binding
 * of the dataspace protocol.
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path(BASE_PATH)
public class DspNegotiationApiController {

    private final DspRequestHandler dspRequestHandler;
    private final String protocol;
    private final ContractNegotiationProtocolService protocolService;

    public DspNegotiationApiController(ContractNegotiationProtocolService protocolService,
                                       DspRequestHandler dspRequestHandler) {

        this(protocolService, dspRequestHandler, DATASPACE_PROTOCOL_HTTP);
    }

    public DspNegotiationApiController(ContractNegotiationProtocolService protocolService,
                                       DspRequestHandler dspRequestHandler,
                                       String protocol) {
        this.protocolService = protocolService;
        this.dspRequestHandler = dspRequestHandler;
        this.protocol = protocol;
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
        var request = GetDspRequest.Builder.newInstance(ContractNegotiation.class, ContractNegotiationError.class)
                .id(id).token(token).serviceCall(protocolService::findById)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(protocol)
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
    public Response initialContractRequest(JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        var request = PostDspRequest.Builder.newInstance(ContractRequestMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyRequested)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(protocol)
                .build();

        return dspRequestHandler.createResource(request);
    }

    /**
     * Consumer-specific endpoint.
     *
     * @param jsonObject dspace:ContractOfferMessage sent by a consumer.
     * @param token      identity token.
     * @return the created contract negotiation or an error.
     */
    @POST
    @Path(INITIAL_CONTRACT_OFFER)
    public Response initialContractOffer(JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        var request = PostDspRequest.Builder.newInstance(ContractOfferMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyOffered)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(protocol)
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
    public Response contractRequest(@PathParam("id") String id,
                                    JsonObject jsonObject,
                                    @HeaderParam(AUTHORIZATION) String token) {
        var request = PostDspRequest.Builder.newInstance(ContractRequestMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyRequested)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(protocol)
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
        var request = PostDspRequest.Builder.newInstance(ContractNegotiationEventMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall((message, claimToken) -> switch (message.getType()) {
                    case FINALIZED -> protocolService.notifyFinalized(message, claimToken);
                    case ACCEPTED -> protocolService.notifyAccepted(message, claimToken);
                })
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(protocol)
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
        var request = PostDspRequest.Builder.newInstance(ContractAgreementVerificationMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyVerified)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(protocol)
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
        var request = PostDspRequest.Builder.newInstance(ContractNegotiationTerminationMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyTerminated)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(protocol)
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
        var request = PostDspRequest.Builder.newInstance(ContractOfferMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE)
                .processId(id)
                .message(body)
                .token(token)
                .serviceCall(protocolService::notifyOffered)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(protocol)
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
        var request = PostDspRequest.Builder.newInstance(ContractAgreementMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE)
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyAgreed)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(protocol)
                .build();

        return dspRequestHandler.updateResource(request);
    }

}
