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

package org.eclipse.edc.connector.controlplane.api.management.asset.v5;

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
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_CATALOG_ASSET_TYPE_TERM;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5beta/participants/{participantContextId}/assets")
public class AssetApiV5Controller implements AssetApiV5 {
    private final TypeTransformerRegistry typeTransformerRegistry;
    private final AssetService assetService;
    private final JsonObjectValidatorRegistry validator;
    private final Monitor monitor;
    private final AuthorizationService authorizationService;

    public AssetApiV5Controller(AssetService assetService, TypeTransformerRegistry managementTypeTransformerRegistry, JsonObjectValidatorRegistry validator, Monitor monitor, AuthorizationService authorizationService) {
        this.assetService = assetService;
        this.typeTransformerRegistry = managementTypeTransformerRegistry;
        this.validator = validator;
        this.monitor = monitor;
        this.authorizationService = authorizationService;
    }

    @POST
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public JsonObject createAssetV5(@PathParam("participantContextId") String participantContextId,
                                    @SchemaType({EDC_ASSET_TYPE_TERM, EDC_CATALOG_ASSET_TYPE_TERM}) JsonObject assetJson,
                                    @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        validator.validate(EDC_ASSET_TYPE, assetJson).orElseThrow(ValidationFailureException::new);


        var asset = typeTransformerRegistry.transform(assetJson, Asset.class)
                .orElseThrow(InvalidRequestException::new)
                .toBuilder()
                .participantContextId(participantContextId)
                .build();

        var idResponse = assetService.create(asset)
                .map(a -> IdResponse.Builder.newInstance()
                        .id(a.getId())
                        .createdAt(a.getCreatedAt())
                        .build())
                .orElseThrow(exceptionMapper(Asset.class, asset.getId()));

        return typeTransformerRegistry.transform(idResponse, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    @POST
    @Path("/request")
    @Override
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    public JsonArray queryAssetsV5(@PathParam("participantContextId") String participantContextId,
                                   @SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson,
                                   @Context SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            validator.validate(EDC_QUERY_SPEC_TYPE, querySpecJson).orElseThrow(ValidationFailureException::new);

            querySpec = typeTransformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        var query = querySpec.toBuilder()
                .filter(new Criterion("participantContextId", "=", participantContextId))
                .build();

        return assetService.search(query).orElseThrow(exceptionMapper(QuerySpec.class, null))
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
    public JsonObject getAssetV5(@PathParam("participantContextId") String participantContextId,
                                 @PathParam("id") String assetId,
                                 @Context SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, assetId, Asset.class)
                .orElseThrow(exceptionMapper(Asset.class, assetId));

        return ofNullable(assetService.findById(assetId))
                .map(asset -> typeTransformerRegistry.transform(asset, JsonObject.class).orElseThrow(f -> new EdcException(f.getFailureDetail())))
                .orElseThrow(() -> new ObjectNotFoundException(Asset.class, assetId));
    }

    @DELETE
    @Path("{assetId}")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public void removeAssetV5(@PathParam("participantContextId") String participantContextId,
                              @PathParam("assetId") String assetId,
                              @Context SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, assetId, Asset.class)
                .orElseThrow(exceptionMapper(Asset.class, assetId));

        assetService.delete(assetId)
                .orElseThrow(exceptionMapper(Asset.class, assetId));
    }

    @PUT
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public void updateAssetV5(@PathParam("participantContextId") String participantContextId,
                              @SchemaType({EDC_ASSET_TYPE_TERM, EDC_CATALOG_ASSET_TYPE_TERM}) JsonObject assetJson,
                              @Context SecurityContext securityContext) {

        validator.validate(EDC_ASSET_TYPE, assetJson).orElseThrow(ValidationFailureException::new);

        var asset = typeTransformerRegistry.transform(assetJson, Asset.class)
                .orElseThrow(InvalidRequestException::new)
                .toBuilder()
                .participantContextId(participantContextId)
                .build();

        var assetId = asset.getId();
        authorizationService.authorize(securityContext, participantContextId, assetId, Asset.class).orElseThrow(exceptionMapper(Asset.class, assetId));

        assetService.update(asset).orElseThrow(exceptionMapper(Asset.class, assetId));

    }

}
