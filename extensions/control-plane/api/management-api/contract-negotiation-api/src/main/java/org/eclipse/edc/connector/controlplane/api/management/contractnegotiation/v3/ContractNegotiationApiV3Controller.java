/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v3;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.BaseContractNegotiationApiController;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v3/contractnegotiations")
public class ContractNegotiationApiV3Controller extends BaseContractNegotiationApiController implements ContractNegotiationApiV3 {
    public ContractNegotiationApiV3Controller(ContractNegotiationService service, TypeTransformerRegistry transformerRegistry, Monitor monitor, JsonObjectValidatorRegistry validatorRegistry) {
        super(service, transformerRegistry, monitor, validatorRegistry);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray queryNegotiationsV3(JsonObject querySpecJson) {
        return queryNegotiations(querySpecJson);
    }

    @GET
    @Path("/{id}")
    @Override
    public JsonObject getNegotiationV3(@PathParam("id") String id) {
        return getNegotiation(id);
    }

    @GET
    @Path("/{id}/state")
    @Override
    public JsonObject getNegotiationStateV3(@PathParam("id") String id) {
        return getNegotiationState(id);
    }

    @GET
    @Path("/{id}/agreement")
    @Override
    public JsonObject getAgreementForNegotiationV3(@PathParam("id") String negotiationId) {
        return getAgreementForNegotiation(negotiationId);
    }

    @POST
    @Override
    public JsonObject initiateContractNegotiationV3(JsonObject requestObject) {
        return initiateContractNegotiation(requestObject);
    }

    @POST
    @Path("/{id}/terminate")
    @Override
    public void terminateNegotiationV3(@PathParam("id") String id, JsonObject terminateNegotiation) {
        terminateNegotiation(id, terminateNegotiation);
    }

    @DELETE
    @Path("/{id}")
    @Override
    public void deleteNegotiationV3(@PathParam("id") String id) {
        delete(id);
    }
}
