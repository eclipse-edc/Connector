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

package org.eclipse.edc.connector.controlplane.api.management.contractdefinition.v5;

import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE_TERM;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5alpha/participants/{participantContextId}/contractdefinitions")
public class ContractDefinitionApiV5Controller implements ContractDefinitionApiV5 {

    private final TypeTransformerRegistry typeTransformerRegistry;
    private final ContractDefinitionService contractDefinitionService;
    private final Monitor monitor;
    private final AuthorizationService authorizationService;

    public ContractDefinitionApiV5Controller(TypeTransformerRegistry typeTransformerRegistry, ContractDefinitionService contractDefinitionService, Monitor monitor, AuthorizationService authorizationService) {
        this.typeTransformerRegistry = typeTransformerRegistry;
        this.contractDefinitionService = contractDefinitionService;
        this.monitor = monitor;
        this.authorizationService = authorizationService;
    }

    @POST
    @Path("/request")
    @Override
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    public JsonArray queryContractDefinitionsV5(@PathParam("participantContextId") String participantContextId,
                                                @SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson,
                                                @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            querySpec = typeTransformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        var query = querySpec.toBuilder()
                .filter(new Criterion("participantContextId", "=", participantContextId))
                .build();

        return contractDefinitionService.search(query).orElseThrow(exceptionMapper(ContractDefinition.class)).stream()
                .map(contractDefinition -> typeTransformerRegistry.transform(contractDefinition, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    @GET
    @Path("/{id}")
    @Override
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    public JsonObject getContractDefinitionV5(@PathParam("participantContextId") String participantContextId,
                                              @PathParam("id") String id,
                                              @Context SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, id, ContractDefinition.class)
                .orElseThrow(exceptionMapper(ContractDefinition.class, id));

        return ofNullable(contractDefinitionService.findById(id))
                .map(cd -> typeTransformerRegistry.transform(cd, JsonObject.class))
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(ContractDefinition.class, id));
    }

    @POST
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public JsonObject createContractDefinitionV5(@PathParam("participantContextId") String participantContextId,
                                                 @SchemaType(CONTRACT_DEFINITION_TYPE_TERM) JsonObject createObject,
                                                 @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        var contractDef = typeTransformerRegistry.transform(createObject, ContractDefinition.class)
                .orElseThrow(InvalidRequestException::new)
                .toBuilder()
                .participantContextId(participantContextId)
                .build();

        var id = contractDefinitionService.create(contractDef)
                .map(cd -> IdResponse.Builder.newInstance()
                        .id(cd.getId())
                        .createdAt(cd.getCreatedAt())
                        .build())
                .orElseThrow(exceptionMapper(ContractDefinition.class));

        return typeTransformerRegistry.transform(id, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public void deleteContractDefinitionV5(@PathParam("participantContextId") String participantContextId,
                                           @PathParam("id") String id,
                                           @Context SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, id, ContractDefinition.class)
                .orElseThrow(exceptionMapper(ContractDefinition.class, id));

        contractDefinitionService.delete(id)
                .orElseThrow(exceptionMapper(ContractDefinition.class, id));
    }

    @PUT
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public void updateContractDefinitionV5(@PathParam("participantContextId") String participantContextId,
                                           @SchemaType(CONTRACT_DEFINITION_TYPE_TERM) JsonObject updateObject,
                                           @Context SecurityContext securityContext) {

        var updateObj = typeTransformerRegistry.transform(updateObject, ContractDefinition.class)
                .orElseThrow(InvalidRequestException::new)
                .toBuilder()
                .participantContextId(participantContextId)
                .build();

        var id = updateObj.getId();

        authorizationService.authorize(securityContext, participantContextId, id, ContractDefinition.class)
                .orElseThrow(exceptionMapper(ContractDefinition.class, id));

        contractDefinitionService.update(updateObj)
                .orElseThrow(exceptionMapper(ContractDefinition.class, id));
    }
}
