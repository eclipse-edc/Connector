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

package org.eclipse.edc.connector.dataplane.selector.api.v5;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.connector.controlplane.dataplane.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5/participants/{participantContextId}/dataplanes")
public class DataplaneSelectorApiV5Controller implements DataplaneSelectorApiV5 {

    private final DataPlaneSelectorService selectionService;
    private final TypeTransformerRegistry transformerRegistry;
    private final Monitor monitor;
    private final AuthorizationService authorizationService;

    public DataplaneSelectorApiV5Controller(DataPlaneSelectorService selectionService, TypeTransformerRegistry transformerRegistry,
                                            Monitor monitor, AuthorizationService authorizationService) {
        this.selectionService = selectionService;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
        this.authorizationService = authorizationService;
    }

    @POST
    @Path("/request")
    @Override
    @RequiredScope("management-api:dataplanes:read")
    public JsonArray queryDataPlaneInstancesV5(@PathParam("participantContextId") String participantContextId,
                                               @SchemaType(value = EDC_QUERY_SPEC_TYPE_TERM, version = "v4") JsonObject querySpecJson,
                                               @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        var query = querySpec.toBuilder()
                .filter(new Criterion("participantContextId", "=", participantContextId))
                .build();

        return selectionService.search(query).orElseThrow(exceptionMapper(DataPlaneInstance.class)).stream()
                .map(instance -> transformerRegistry.transform(instance, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }
}
