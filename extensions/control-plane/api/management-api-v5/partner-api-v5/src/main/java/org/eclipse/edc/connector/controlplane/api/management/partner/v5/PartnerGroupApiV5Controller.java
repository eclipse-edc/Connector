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

package org.eclipse.edc.connector.controlplane.api.management.partner.v5;

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
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.connector.controlplane.services.spi.partner.PartnerGroupService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.connector.controlplane.api.management.partner.v5.PartnerApiV5Controller.READ_SCOPE;
import static org.eclipse.edc.connector.controlplane.api.management.partner.v5.PartnerApiV5Controller.WRITE_SCOPE;
import static org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup.EDC_PARTNER_GROUP_TYPE_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5/participants/{participantContextId}/partnergroups")
public class PartnerGroupApiV5Controller implements PartnerGroupApiV5 {

    private final PartnerGroupService partnerGroupService;
    private final TypeTransformerRegistry transformerRegistry;
    private final Monitor monitor;
    private final AuthorizationService authorizationService;

    public PartnerGroupApiV5Controller(PartnerGroupService partnerGroupService, TypeTransformerRegistry transformerRegistry, Monitor monitor, AuthorizationService authorizationService) {
        this.partnerGroupService = partnerGroupService;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
        this.authorizationService = authorizationService;
    }

    @POST
    @Path("request")
    @RequiredScope(READ_SCOPE)
    @Override
    public JsonArray queryPartnerGroupsV5(@PathParam("participantContextId") String participantContextId,
                                          @SchemaType(value = EDC_QUERY_SPEC_TYPE_TERM, version = "v4") JsonObject querySpecJson,
                                          @Context SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        var querySpec = querySpecJson == null
                ? QuerySpec.Builder.newInstance().build()
                : transformerRegistry.transform(querySpecJson, QuerySpec.class).orElseThrow(InvalidRequestException::new);

        var query = querySpec.toBuilder().filter(filterByParticipantContextId(participantContextId)).build();

        return partnerGroupService.search(query).orElseThrow(exceptionMapper(QuerySpec.class, null))
                .stream()
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    @GET
    @Path("{id}")
    @RequiredScope(READ_SCOPE)
    @Override
    public JsonObject getPartnerGroupV5(@PathParam("participantContextId") String participantContextId,
                                        @PathParam("id") String id,
                                        @Context SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, id, PartnerGroup.class)
                .orElseThrow(exceptionMapper(PartnerGroup.class, id));

        var group = partnerGroupService.findById(participantContextId, id)
                .orElseThrow(exceptionMapper(PartnerGroup.class, id));

        return transformerRegistry.transform(group, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @POST
    @RequiredScope(WRITE_SCOPE)
    @Override
    public JsonObject createPartnerGroupV5(@PathParam("participantContextId") String participantContextId,
                                           @SchemaType(value = EDC_PARTNER_GROUP_TYPE_TERM, version = "v5") JsonObject groupJson,
                                           @Context SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        var group = transformerRegistry.transform(groupJson, PartnerGroup.class)
                .orElseThrow(InvalidRequestException::new)
                .toBuilder()
                .participantContextId(participantContextId)
                .build();

        var created = partnerGroupService.create(group)
                .onSuccess(g -> monitor.debug("PartnerGroup created %s".formatted(g.getId())))
                .orElseThrow(exceptionMapper(PartnerGroup.class, group.getId()));

        var idResponse = IdResponse.Builder.newInstance().id(created.getId()).createdAt(created.getCreatedAt()).build();
        return transformerRegistry.transform(idResponse, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @DELETE
    @Path("{id}")
    @RequiredScope(WRITE_SCOPE)
    @Override
    public void deletePartnerGroupV5(@PathParam("participantContextId") String participantContextId,
                                     @PathParam("id") String id,
                                     @Context SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, id, PartnerGroup.class)
                .orElseThrow(exceptionMapper(PartnerGroup.class, id));

        partnerGroupService.delete(participantContextId, id)
                .onSuccess(g -> monitor.debug("PartnerGroup deleted %s".formatted(g.getId())))
                .orElseThrow(exceptionMapper(PartnerGroup.class, id));
    }

    @PUT
    @Path("{id}")
    @RequiredScope(WRITE_SCOPE)
    @Override
    public void updatePartnerGroupV5(@PathParam("participantContextId") String participantContextId,
                                     @PathParam("id") String id,
                                     @SchemaType(value = EDC_PARTNER_GROUP_TYPE_TERM, version = "v5") JsonObject groupJson,
                                     @Context SecurityContext securityContext) {
        var builder = transformerRegistry.transform(groupJson, PartnerGroup.class)
                .orElseThrow(InvalidRequestException::new)
                .toBuilder()
                .participantContextId(participantContextId);

        if (!groupJson.containsKey(ID)) {
            builder.id(id);
        }

        var group = builder.build();
        if (!id.equals(group.getId())) {
            throw new InvalidRequestException("PartnerGroup id in the request body (%s) does not match the one in the path (%s)".formatted(group.getId(), id));
        }

        authorizationService.authorize(securityContext, participantContextId, id, PartnerGroup.class)
                .orElseThrow(exceptionMapper(PartnerGroup.class, id));

        partnerGroupService.update(group)
                .onSuccess(g -> monitor.debug("PartnerGroup updated %s".formatted(g.getId())))
                .orElseThrow(exceptionMapper(PartnerGroup.class, id));
    }
}
