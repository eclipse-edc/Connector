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

package org.eclipse.edc.connector.controlplane.api.management.contractagreement.v5;

import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.connector.controlplane.api.management.contractagreement.BaseContractAgreementApiV5Controller;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5beta/participants/{participantContextId}/contractagreements")
public class ContractAgreementApiV5Controller extends BaseContractAgreementApiV5Controller implements ContractAgreementApiV5 {
    public ContractAgreementApiV5Controller(ContractAgreementService service, AuthorizationService authorizationService, TypeTransformerRegistry transformerRegistry, Monitor monitor, JsonObjectValidatorRegistry validatorRegistry) {
        super(service, authorizationService, transformerRegistry, monitor, validatorRegistry);
    }

    @POST
    @Path("/request")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonArray queryAgreementsV5(@PathParam("participantContextId") String participantContextId,
                                       @SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson,
                                       @Context SecurityContext securityContext) {
        return queryAgreements(participantContextId, querySpecJson, securityContext);
    }

    @GET
    @Path("{id}")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonObject getAgreementByIdV5(@PathParam("participantContextId") String participantContextId,
                                         @PathParam("id") String id,
                                         @Context SecurityContext securityContext) {
        return getAgreementById(participantContextId, id, securityContext);
    }

    @GET
    @Path("{id}/negotiation")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonObject getNegotiationByAgreementIdV5(@PathParam("participantContextId") String participantContextId,
                                                    @PathParam("id") String id,
                                                    @Context SecurityContext securityContext) {
        return getNegotiationByAgreementId(participantContextId, id, securityContext);
    }
}
