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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v4;

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
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand.TERMINATE_NEGOTIATION_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_TYPE_TERM;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4beta/contractnegotiations")
public class ContractNegotiationApiV4Controller extends BaseContractNegotiationApiController implements ContractNegotiationApiV4 {
    public ContractNegotiationApiV4Controller(ContractNegotiationService service, TypeTransformerRegistry transformerRegistry, Monitor monitor,
                                              JsonObjectValidatorRegistry validatorRegistry, SingleParticipantContextSupplier participantContextSupplier) {
        super(service, transformerRegistry, monitor, validatorRegistry, participantContextSupplier);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray queryNegotiationsV4(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson) {
        return queryNegotiations(querySpecJson);
    }

    @GET
    @Path("/{id}")
    @Override
    public JsonObject getNegotiationV4(@PathParam("id") String id) {
        return getNegotiation(id);
    }

    @GET
    @Path("/{id}/state")
    @Override
    public JsonObject getNegotiationStateV4(@PathParam("id") String id) {
        return getNegotiationState(id);
    }

    @GET
    @Path("/{id}/agreement")
    @Override
    public JsonObject getAgreementForNegotiationV4(@PathParam("id") String negotiationId) {
        return getAgreementForNegotiation(negotiationId);
    }

    @POST
    @Override
    public JsonObject initiateContractNegotiationV4(@SchemaType(CONTRACT_REQUEST_TYPE_TERM) JsonObject requestObject) {
        return initiateContractNegotiation(requestObject);
    }

    @POST
    @Path("/{id}/terminate")
    @Override
    public void terminateNegotiationV4(@PathParam("id") String id, @SchemaType(TERMINATE_NEGOTIATION_TYPE_TERM) JsonObject terminateNegotiation) {
        terminateNegotiation(id, terminateNegotiation);
    }

    @DELETE
    @Path("/{id}")
    @Override
    public void deleteNegotiationV4(@PathParam("id") String id) {
        delete(id);
    }
}
