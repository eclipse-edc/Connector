/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.http.api.v2025.virtual.controller;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
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
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.http.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.PostDspRequest;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ParticipantProfileResolver;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.CONTRACT_OFFERS;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.EVENT;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.INITIAL_CONTRACT_OFFERS;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.TERMINATION;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.VERIFICATION;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1_VERSION;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM;

/**
 * Versioned Negotiation endpoint for 2025/1 protocol version. Path is scoped by participant
 * context id, profile id and DSP protocol version segment. The version segment dispatches to
 * this controller class; the profile determines the JSON-LD namespace and protocol string used
 * to dispatch the request. The profile's DSP version must match this controller's version.
 * The controller is not actually registered directly on the web service, but is returned by the DspVirtualProfileDispatcher when
 * a request matches the path parameters and version. This allows for multiple versions of the same API to be supported simultaneously.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/{participantContextId}/{profileId}" + BASE_PATH)
public class DspVirtualNegotiationApiController20251 {

    private final ContractNegotiationProtocolService protocolService;
    private final ParticipantContextService participantContextService;
    private final ParticipantProfileResolver profileResolver;
    private final DspRequestHandler dspRequestHandler;

    public DspVirtualNegotiationApiController20251(ContractNegotiationProtocolService protocolService,
                                                   ParticipantContextService participantContextService,
                                                   ParticipantProfileResolver profileResolver,
                                                   DspRequestHandler dspRequestHandler) {
        this.protocolService = protocolService;
        this.participantContextService = participantContextService;
        this.profileResolver = profileResolver;
        this.dspRequestHandler = dspRequestHandler;
    }

    @POST
    @Path("{id}" + CONTRACT_OFFERS)
    public Response providerOffer(@PathParam("participantContextId") String participantContextId,
                                  @PathParam("profileId") String profileId,
                                  @PathParam("id") String id, JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var request = PostDspRequest.Builder.newInstance(ContractOfferMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(profile.protocolNamespace().toIri(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM))
                .processId(id)
                .message(body)
                .token(token)
                .serviceCall(protocolService::notifyOffered)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(profile.name())
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.updateResource(request);
    }

    @POST
    @Path(INITIAL_CONTRACT_OFFERS)
    public Response initialContractOffer(@PathParam("participantContextId") String participantContextId,
                                         @PathParam("profileId") String profileId,
                                         JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var request = PostDspRequest.Builder.newInstance(ContractOfferMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(profile.protocolNamespace().toIri(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM))
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyOffered)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(profile.name())
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.createResource(request);
    }

    @GET
    @Path("{id}")
    public Response getNegotiation(@PathParam("participantContextId") String participantContextId,
                                   @PathParam("profileId") String profileId,
                                   @PathParam("id") String id, @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var protocol = profile.name();
        var message = ContractNegotiationRequestMessage.Builder.newInstance()
                .negotiationId(id)
                .protocol(protocol)
                .build();

        var request = GetDspRequest.Builder.newInstance(ContractNegotiationRequestMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .id(id)
                .message(message)
                .token(token)
                .serviceCall(protocolService::findById)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(protocol)
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.getResource(request);
    }

    @POST
    @Path(INITIAL_CONTRACT_REQUEST)
    public Response initialContractRequest(@PathParam("participantContextId") String participantContextId,
                                           @PathParam("profileId") String profileId,
                                           JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var request = PostDspRequest.Builder.newInstance(ContractRequestMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(profile.protocolNamespace().toIri(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM))
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyRequested)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(profile.name())
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.createResource(request);
    }

    @POST
    @Path("{id}" + CONTRACT_REQUEST)
    public Response contractRequest(@PathParam("participantContextId") String participantContextId,
                                    @PathParam("profileId") String profileId,
                                    @PathParam("id") String id,
                                    JsonObject jsonObject,
                                    @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var request = PostDspRequest.Builder.newInstance(ContractRequestMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(profile.protocolNamespace().toIri(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM))
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyRequested)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(profile.name())
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.updateResource(request);
    }

    @POST
    @Path("{id}" + EVENT)
    public Response createEvent(@PathParam("participantContextId") String participantContextId,
                                @PathParam("profileId") String profileId,
                                @PathParam("id") String id,
                                JsonObject jsonObject,
                                @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var request = PostDspRequest.Builder.newInstance(ContractNegotiationEventMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(profile.protocolNamespace().toIri(DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM))
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall((ctx, message, claimToken) -> switch (message.getType()) {
                    case FINALIZED -> protocolService.notifyFinalized(ctx, message, claimToken);
                    case ACCEPTED -> protocolService.notifyAccepted(ctx, message, claimToken);
                })
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(profile.name())
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.updateResource(request);
    }

    @POST
    @Path("{id}" + AGREEMENT + VERIFICATION)
    public Response verifyAgreement(@PathParam("participantContextId") String participantContextId,
                                    @PathParam("profileId") String profileId,
                                    @PathParam("id") String id,
                                    JsonObject jsonObject,
                                    @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var request = PostDspRequest.Builder.newInstance(ContractAgreementVerificationMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(profile.protocolNamespace().toIri(DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_TERM))
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyVerified)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(profile.name())
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.updateResource(request);
    }

    @POST
    @Path("{id}" + TERMINATION)
    public Response terminateNegotiation(@PathParam("participantContextId") String participantContextId,
                                         @PathParam("profileId") String profileId,
                                         @PathParam("id") String id,
                                         JsonObject jsonObject,
                                         @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var request = PostDspRequest.Builder.newInstance(ContractNegotiationTerminationMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(profile.protocolNamespace().toIri(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM))
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyTerminated)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(profile.name())
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.updateResource(request);
    }

    @POST
    @Path("{id}" + AGREEMENT)
    public Response createAgreement(@PathParam("participantContextId") String participantContextId,
                                    @PathParam("profileId") String profileId,
                                    @PathParam("id") String id,
                                    JsonObject jsonObject,
                                    @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var request = PostDspRequest.Builder.newInstance(ContractAgreementMessage.class, ContractNegotiation.class, ContractNegotiationError.class)
                .expectedMessageType(profile.protocolNamespace().toIri(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM))
                .processId(id)
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyAgreed)
                .errorProvider(ContractNegotiationError.Builder::newInstance)
                .protocol(profile.name())
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.updateResource(request);
    }

    private DataspaceProfileContext resolveProfile(String participantContextId, String profileId) {
        var profile = profileResolver.resolve(participantContextId, profileId)
                .orElseThrow(() -> new NotFoundException("No profile '%s' for participant '%s'".formatted(profileId, participantContextId)));
        if (!V_2025_1_VERSION.equals(profile.protocolVersion().version())) {
            throw new NotFoundException("Profile '%s' is not for DSP version %s".formatted(profileId, V_2025_1_VERSION));
        }
        return profile;
    }

    private ParticipantContextSupplier participantContextSupplier(String id) {
        return () -> participantContextService.getParticipantContext(id);
    }
}
