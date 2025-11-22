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

package org.eclipse.edc.connector.controlplane.api.management.contractagreement.v4;

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
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4beta/contractagreements")
public class ContractAgreementApiV4Controller extends BaseContractAgreementApiController implements ContractAgreementApiV4 {
    public ContractAgreementApiV4Controller(ContractAgreementService service, TypeTransformerRegistry transformerRegistry, Monitor monitor, JsonObjectValidatorRegistry validatorRegistry) {
        super(service, transformerRegistry, monitor, validatorRegistry);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray queryAgreementsV4(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson) {
        return queryAgreements(querySpecJson);
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getAgreementByIdV4(@PathParam("id") String id) {
        return getAgreementById(id);
    }

    @GET
    @Path("{id}/negotiation")
    @Override
    public JsonObject getNegotiationByAgreementIdV4(@PathParam("id") String id) {
        return getNegotiationByAgreementId(id);
    }
}
