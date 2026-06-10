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

package org.eclipse.edc.connector.controlplane.api.management.discovery.v5;

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
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest.DISCOVERY_REQUEST_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5beta/participants/{participantContextId}/discover")
public class DiscoveryApiV5Controller implements DiscoveryApiV5 {

    private final AuthorizationService authorizationService;
    private final DiscoveryService discoveryService;
    private final TypeTransformerRegistry transformerRegistry;
    private final Monitor monitor;

    public DiscoveryApiV5Controller(AuthorizationService authorizationService,
                                    DiscoveryService discoveryService,
                                    TypeTransformerRegistry transformerRegistry,
                                    Monitor monitor) {
        this.authorizationService = authorizationService;
        this.discoveryService = discoveryService;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
    }

    @POST
    @Path("/request")
    @RequiredScope("management-api:discovery:read")
    @Override
    public JsonArray discoverV5(@PathParam("participantContextId") String participantContextId,
                                @SchemaType(DISCOVERY_REQUEST_TYPE_TERM) JsonObject request,
                                @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        var discoveryRequest = transformerRegistry.transform(request, DiscoveryRequest.class)
                .orElseThrow(InvalidRequestException::new);

        var matches = discoveryService.discover(participantContextId, discoveryRequest)
                .orElseThrow(exceptionMapper(DiscoveryRequest.class, participantContextId));

        return matches.stream()
                .map(match -> transformerRegistry.transform(match, JsonObject.class)
                        .onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }
}
