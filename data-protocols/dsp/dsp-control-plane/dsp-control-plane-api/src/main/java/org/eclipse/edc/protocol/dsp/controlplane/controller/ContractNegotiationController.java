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

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.Map;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/negotiations")
public class ContractNegotiationController {

    // Provider

    @GET
    @Path("/{id}")
    public Map<String, Object> getNegotiation(@PathParam("id") String id) {
        return new HashMap<>();
    }

    @POST
    @Path("/request")
    public Map<String, Object> initiateNegotiation(JsonObject jsonObject) {
        return new HashMap<>();
    }

    @POST
    @Path("/{id}/request")
    public void consumerOffer(JsonObject jsonObject) { }

    @POST
    @Path("/{id}/events")
    public void acceptCurrentOffer() { }

    @POST
    @Path("/{id}/agreement/verification")
    public void verifyAgreement(JsonObject jsonObject) { }

    @POST
    @Path("/{id}/termination")
    public void terminateNegotiation() { }

    // Consumer

    @POST
    @Path("/{id}/offers")
    public void providerOffer(JsonObject jsonObject) { }

    @POST
    @Path("/{id}/agreement")
    public void sendAgreement(JsonObject jsonObject) { }

    @POST
    @Path("/{id}/events")
    public void finalizeAgreement(JsonObject jsonObject) { }

}
