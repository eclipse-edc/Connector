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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v2;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
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
import static org.eclipse.edc.api.ApiWarnings.deprecationWarning;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v2/contractnegotiations")
public class ContractNegotiationApiV2Controller extends BaseContractNegotiationApiController implements ContractNegotiationApiV2 {
    public ContractNegotiationApiV2Controller(ContractNegotiationService service, TypeTransformerRegistry transformerRegistry, Monitor monitor, JsonObjectValidatorRegistry validatorRegistry) {
        super(service, transformerRegistry, monitor, validatorRegistry);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray queryNegotiationsV2(JsonObject querySpecJson) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return queryNegotiations(querySpecJson);
    }

    @GET
    @Path("/{id}")
    @Override
    public JsonObject getNegotiationV2(@PathParam("id") String id) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return getNegotiation(id);
    }

    @GET
    @Path("/{id}/state")
    @Override
    public JsonObject getNegotiationStateV2(@PathParam("id") String id) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return getNegotiationState(id);
    }

    @GET
    @Path("/{id}/agreement")
    @Override
    public JsonObject getAgreementForNegotiationV2(@PathParam("id") String negotiationId) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return getAgreementForNegotiation(negotiationId);
    }

    @POST
    @Override
    public JsonObject initiateContractNegotiationV2(JsonObject requestObject) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return initiateContractNegotiation(requestObject);
    }

    @POST
    @Path("/{id}/terminate")
    @Override
    public void terminateNegotiationV2(@PathParam("id") String id, JsonObject terminateNegotiation) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        terminateNegotiation(id, terminateNegotiation);
    }
}
