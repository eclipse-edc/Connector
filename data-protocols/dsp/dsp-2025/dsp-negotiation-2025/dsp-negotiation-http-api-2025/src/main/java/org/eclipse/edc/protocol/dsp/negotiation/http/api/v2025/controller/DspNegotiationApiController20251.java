/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.protocol.dsp.negotiation.http.api.v2025.controller;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.negotiation.http.api.controller.BaseDspNegotiationApiController;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.CONTRACT_OFFERS;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.INITIAL_CONTRACT_OFFERS;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1_PATH;

/**
 * Versioned Negotiation endpoint for 2024/1 protocol version
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path(V_2025_1_PATH + BASE_PATH)
public class DspNegotiationApiController20251 extends BaseDspNegotiationApiController {

    public DspNegotiationApiController20251(ContractNegotiationProtocolService protocolService, DspRequestHandler dspRequestHandler, SingleParticipantContextSupplier participantContextSupplier) {
        super(protocolService, dspRequestHandler, participantContextSupplier, DATASPACE_PROTOCOL_HTTP_V_2025_1, DSP_NAMESPACE_V_2025_1);
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
    @Path("{id}" + CONTRACT_OFFERS)
    @Override
    public Response providerOffer(@PathParam("id") String id, JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        return super.providerOffer(id, body, token);
    }

    /**
     * Consumer-specific endpoint.
     *
     * @param jsonObject dspace:ContractOfferMessage sent by a consumer.
     * @param token      identity token.
     * @return the created contract negotiation or an error.
     */
    @POST
    @Path(INITIAL_CONTRACT_OFFERS)
    @Override
    public Response initialContractOffer(JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        return super.initialContractOffer(jsonObject, token);
    }
}
