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
import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.CONTRACT_OFFER;
import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.EVENT;
import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.INITIAL_CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.TERMINATION;
import static org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationPath.VERIFICATION;
import static org.eclipse.edc.protocol.dsp.transform.util.DocumentUtil.compactDocument;
import static org.eclipse.edc.protocol.dsp.transform.util.DocumentUtil.expandDocument;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path(BASE_PATH)
public class ContractNegotiationController implements ContractNegotiationApi {

    private final Monitor monitor;

    private final DspContractNegotiationService service;

    private final ObjectMapper mapper;

    public ContractNegotiationController(Monitor monitor, DspContractNegotiationService service, TypeManager typeManager) {
        this.monitor = monitor;
        this.service = service;
        this.mapper = typeManager.getMapper("json-ld");
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getNegotiation(@PathParam("id") String id) {
        monitor.debug(format("DSP: Incoming request for contract negotiation with id %s", id));

        return mapper.convertValue(compactDocument(service.getNegotiationById(id)), JsonObject.class);
    }

    @POST
    @Path(INITIAL_CONTRACT_REQUEST)
    @Override
    public JsonObject initiateNegotiation(@RequestBody(description = "dspace:ContractRequestMessage", required = true) JsonObject body) {
        monitor.debug("DSP: Incoming ContractRequestMessage for initiating a contract negotiation.");

        var negotiation = service.createNegotiation(expandDocument(body).getJsonObject(0));
        return mapper.convertValue(compactDocument(negotiation), JsonObject.class);
    }

    @POST
    @Path("{id}" + CONTRACT_REQUEST)
    @Override
    public void consumerOffer(@PathParam("id") String id, @RequestBody(description = "dspace:ContractRequestMessage", required = true) JsonObject body) {
        monitor.debug(format("DSP: Incoming ContractRequestMessage for process %s", id));

        service.consumerOffer(id, expandDocument(body).getJsonObject(0));
    }


    @POST
    @Path("{id}" + EVENT)
    @Override // finalize agreement, acceptCurrentOffer
    public void createEvent(@PathParam("id") String id, @RequestBody(description = "dspace:ContractNegotiationEventMessage", required = true) JsonObject body) {
        monitor.debug(format("DSP: Incoming ContractNegotiationEventMessage for process %s", id));

        service.processEvent(id, expandDocument(body).getJsonObject(0));
    }

    @POST
    @Path("{id}" + AGREEMENT + VERIFICATION)
    @Override
    public void verifyAgreement(@PathParam("id") String id, @RequestBody(description = "dspace:ContractAgreementVerificationMessage", required = true) JsonObject body) {
        monitor.debug(format("DSP: Incoming ContractAgreementVerificationMessage for process %s", id));

        service.verifyAgreement(id, expandDocument(body).getJsonObject(0));
    }

    @POST
    @Path("{id}" + TERMINATION)
    @Override
    public void terminateNegotiation(@PathParam("id") String id, @RequestBody(description = "dspace:ContractNegotiationTerminationMessage", required = true) JsonObject body) {
        monitor.debug(format("DSP: Incoming ContractNegotiationTerminationMessage for process %s", id));

        service.terminateNegotiation(id, expandDocument(body).getJsonObject(0));
    }

    @POST
    @Path("{id}" + CONTRACT_OFFER)

    @Override
    public void providerOffer(@PathParam("id") String id, @RequestBody(description = "dspace:ContractOfferMessage", required = true) JsonObject body) {
        monitor.debug(format("DSP: Incoming ContractOfferMessage for process %s", id));

        service.providerOffer(id, expandDocument(body).getJsonObject(0));
    }

    @POST
    @Path("{id}" + AGREEMENT)
    @Override
    public void createAgreement(@PathParam("id") String id, @RequestBody(description = "dspace:ContractAgreementMessage", required = true) JsonObject body) {
        monitor.debug(format("DSP: Incoming ContractAgreementMessage for process %s", id));

        service.createAgreement(id, expandDocument(body).getJsonObject(0));
    }
}
