/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.api.query.v5;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.catalog.api.query.BaseCatalogsApiController;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5/participants/{participantContextId}/catalogs")
public class CatalogsApiV5Controller extends BaseCatalogsApiController implements CatalogsApiV5 {

    private final AuthorizationService authorizationService;

    public CatalogsApiV5Controller(QueryService queryService, TypeTransformerRegistry transformerRegistry, AuthorizationService authorizationService) {
        super(queryService, transformerRegistry);
        this.authorizationService = authorizationService;
    }

    @Override
    @POST
    @Path("/request")
    @RequiredScope("management-api:catalogs:read")
    public JsonArray requestCatalogsV5(@PathParam("participantContextId") String participantContextId,
                                       @SchemaType(value = EDC_QUERY_SPEC_TYPE_TERM, version = "v4") JsonObject querySpecJson,
                                       @DefaultValue("false") @QueryParam("flatten") boolean flatten,
                                       @Context SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        return requestCatalogs(participantContextId, querySpecJson, flatten);
    }
}
