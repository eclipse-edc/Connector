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

package org.eclipse.edc.connector.controlplane.api.management.policy.v5;

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
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyEvaluationPlanRequest;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyValidationResult;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
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

import java.util.ArrayList;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.lang.String.format;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE_TERM;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5alpha/participants/{participantContextId}/policydefinitions")
public class PolicyDefinitionApiV5Controller implements PolicyDefinitionApiV5 {
    private final TypeTransformerRegistry typeTransformerRegistry;
    private final Monitor monitor;
    private final AuthorizationService authorizationService;
    private final PolicyDefinitionService policyDefinitionService;

    public PolicyDefinitionApiV5Controller(PolicyDefinitionService policyDefinitionService, TypeTransformerRegistry transformerRegistry, Monitor monitor, AuthorizationService authorizationService) {
        this.policyDefinitionService = policyDefinitionService;
        this.typeTransformerRegistry = transformerRegistry;
        this.monitor = monitor;
        this.authorizationService = authorizationService;
    }


    @POST
    @Path("request")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonArray queryPolicyDefinitionsV5(@PathParam("participantContextId") String participantContextId,
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

        return policyDefinitionService.search(query).orElseThrow(exceptionMapper(QuerySpec.class, null))
                .stream()
                .map(it -> typeTransformerRegistry.transform(it, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    @GET
    @Path("{id}")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonObject getPolicyDefinitionV5(@PathParam("participantContextId") String participantContextId,
                                            @PathParam("id") String id,
                                            @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, id, PolicyDefinition.class)
                .orElseThrow(exceptionMapper(PolicyDefinition.class, id));

        var definition = policyDefinitionService.findById(id);
        if (definition == null) {
            throw new ObjectNotFoundException(PolicyDefinition.class, id);
        }

        return typeTransformerRegistry.transform(definition, JsonObject.class)
                .orElseThrow(failure -> new ObjectNotFoundException(PolicyDefinition.class, id));
    }

    @POST
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public JsonObject createPolicyDefinitionV5(@PathParam("participantContextId") String participantContextId,
                                               @SchemaType(EDC_POLICY_DEFINITION_TYPE_TERM) JsonObject policyDefinition,
                                               @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        var definition = typeTransformerRegistry.transform(policyDefinition, PolicyDefinition.class)
                .orElseThrow(InvalidRequestException::new)
                .toBuilder()
                .participantContextId(participantContextId)
                .build();

        var createdDefinition = policyDefinitionService.create(definition)
                .onSuccess(d -> monitor.debug(format("Policy Definition created %s", d.getId())))
                .orElseThrow(exceptionMapper(PolicyDefinition.class, definition.getId()));

        var responseDto = IdResponse.Builder.newInstance()
                .id(createdDefinition.getId())
                .createdAt(createdDefinition.getCreatedAt())
                .build();

        return typeTransformerRegistry.transform(responseDto, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public void deletePolicyDefinitionV5(@PathParam("participantContextId") String participantContextId,
                                         @PathParam("id") String id,
                                         @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, id, PolicyDefinition.class)
                .orElseThrow(exceptionMapper(Asset.class, id));

        policyDefinitionService.deleteById(id)
                .onSuccess(d -> monitor.debug(format("Policy Definition deleted %s", d.getId())))
                .orElseThrow(exceptionMapper(PolicyDefinition.class, id));
    }

    @PUT
    @Path("{id}")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public void updatePolicyDefinitionV5(@PathParam("participantContextId") String participantContextId,
                                         @PathParam("id") String id,
                                         @SchemaType(EDC_POLICY_DEFINITION_TYPE_TERM) JsonObject input,
                                         @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, id, PolicyDefinition.class)
                .orElseThrow(exceptionMapper(PolicyDefinition.class, id));

        var policyDefinition = typeTransformerRegistry.transform(input, PolicyDefinition.class)
                .orElseThrow(InvalidRequestException::new)
                .toBuilder()
                .participantContextId(participantContextId)
                .build();

        policyDefinitionService.update(policyDefinition)
                .onSuccess(d -> monitor.debug(format("Policy Definition updated %s", d.getId())))
                .orElseThrow(exceptionMapper(PolicyDefinition.class, id));
    }

    @POST
    @Path("{id}/validate")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonObject validatePolicyDefinitionV5(@PathParam("participantContextId") String participantContextId,
                                                 @PathParam("id") String id,
                                                 @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, id, PolicyDefinition.class)
                .orElseThrow(exceptionMapper(PolicyDefinition.class, id));

        var definition = policyDefinitionService.findById(id);
        if (definition == null) {
            throw new ObjectNotFoundException(PolicyDefinition.class, id);
        }

        var messages = new ArrayList<String>();

        var result = policyDefinitionService.validate(definition.getPolicy())
                .onFailure(failure -> messages.addAll(failure.getMessages()));

        var validationResult = new PolicyValidationResult(result.succeeded(), messages);

        return typeTransformerRegistry.transform(validationResult, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @POST
    @Path("{id}/evaluationplan")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonObject createExecutionPlanV5(@PathParam("participantContextId") String participantContextId,
                                            @PathParam("id") String id,
                                            @SchemaType("PolicyEvaluationPlanRequest") JsonObject input,
                                            @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, id, PolicyDefinition.class)
                .orElseThrow(exceptionMapper(PolicyDefinition.class, id));

        var planeRequest = typeTransformerRegistry.transform(input, PolicyEvaluationPlanRequest.class)
                .orElseThrow(InvalidRequestException::new);

        var definition = policyDefinitionService.findById(id);
        if (definition == null) {
            throw new ObjectNotFoundException(PolicyDefinition.class, id);
        }

        var plan = policyDefinitionService.createEvaluationPlan(planeRequest.policyScope(), definition.getPolicy())
                .orElseThrow(exceptionMapper(PolicyDefinition.class, definition.getId()));

        return typeTransformerRegistry.transform(plan, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }
}
