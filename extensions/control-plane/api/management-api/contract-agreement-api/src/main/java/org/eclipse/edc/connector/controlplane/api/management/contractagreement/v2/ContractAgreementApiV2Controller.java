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

package org.eclipse.edc.connector.controlplane.api.management.contractagreement.v2;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.connector.controlplane.api.management.contractagreement.BaseContractAgreementApiController;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.api.ApiWarnings.deprecationWarning;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v2/contractagreements")
public class ContractAgreementApiV2Controller extends BaseContractAgreementApiController implements ContractAgreementApiV2 {
    public ContractAgreementApiV2Controller(ContractAgreementService service, TypeTransformerRegistry transformerRegistry, Monitor monitor, JsonObjectValidatorRegistry validatorRegistry) {
        super(service, transformerRegistry, monitor, validatorRegistry);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray queryAgreementsV2(JsonObject querySpecJson) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return queryAgreements(querySpecJson);
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getAgreementByIdV2(@PathParam("id") String id) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return getAgreementById(id);
    }

    @GET
    @Path("{id}/negotiation")
    @Override
    public JsonObject getNegotiationByAgreementIdV2(@PathParam("id") String id) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return getNegotiationByAgreementId(id);
    }
}
