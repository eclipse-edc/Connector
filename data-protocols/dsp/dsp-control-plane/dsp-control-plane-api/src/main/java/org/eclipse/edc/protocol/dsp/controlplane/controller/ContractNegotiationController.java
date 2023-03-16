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

package org.eclipse.edc.protocol.dsp.controlplane.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.protocol.dsp.spi.controlplane.service.DspContractNegotiationService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.JsonLdUtil.compactDocument;
import static org.eclipse.edc.jsonld.JsonLdUtil.expandDocument;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/negotiations")
public class ContractNegotiationController implements ContractNegotiationApiProvider, ContractNegotiationApiConsumer {

    private final Monitor monitor;

    private final DspContractNegotiationService service;

    private final ObjectMapper mapper;

    public ContractNegotiationController(Monitor monitor, DspContractNegotiationService service, TypeManager typeManager) {
        this.monitor = monitor;
        this.service = service;
        this.mapper = typeManager.getMapper("json-ld");
    }

    // Provider

    @GET
    @Path("/{id}")
    @Override
    public JsonObject getNegotiation(@PathParam("id") String id) {
        monitor.debug(format("DSP: Incoming request for contract negotiation with id %s", id));

        return service.getNegotiationById(id);
    }

    @POST
    @Path("/request")
    @Override
    public JsonObject initiateNegotiation(@RequestBody(description = "dspace:ContractRequestMessage", required = true) JsonObject body) {
        monitor.debug("DSP: Contract negotiation process started.");
        var contractRequest = expandDocument(body).getJsonObject(0);

        var negotiation = service.createNegotiation(contractRequest);

        return mapper.convertValue(compactDocument(negotiation), JsonObject.class);
    }

    @POST
    @Path("/{id}/request")
    @Override
    public void consumerOffer(@PathParam("id") String id, @RequestBody(description = "dspace:ContractRequestMessage", required = true) JsonObject body) {
        monitor.debug(format("DSP: Incoming contract offer for contract negotiation process with id %s", id));
    }


    @POST
    @Path("/{id}/events")
    @Override
    public void acceptCurrentOffer(@PathParam("id") String id, @RequestBody(description = "dspace:ContractNegotiationEventMessage", required = true) JsonObject body) {

    }

    @POST
    @Path("/{id}/agreement/verification")
    @Override
    public void verifyAgreement(@PathParam("id") String id, @RequestBody(description = "dspace:ContractAgreementVerificationMessage", required = true) JsonObject body) {

    }

    @POST
    @Path("/{id}/termination")
    @Override
    public void terminateNegotiation(@PathParam("id") String id) {

    }

    // Consumer

    @POST
    @Path("/{id}/offers")

    @Override
    public void providerOffer(@PathParam("id") String id, @RequestBody(description = "dspace:ContractOfferMessage", required = true) JsonObject body) {

    }

    @POST
    @Path("/{id}/agreement")
    @Override
    public void createAgreement(@PathParam("id") String id, @RequestBody(description = "dspace:ContractAgreementMessage", required = true) JsonObject body) {

    }

    @POST
    @Path("/{id}/events")
    @Override
    public void finalizeAgreement(@PathParam("id") String id, @RequestBody(description = "dspace:ContractNegotiationEventMessage", required = true) JsonObject body) {

    }
}
