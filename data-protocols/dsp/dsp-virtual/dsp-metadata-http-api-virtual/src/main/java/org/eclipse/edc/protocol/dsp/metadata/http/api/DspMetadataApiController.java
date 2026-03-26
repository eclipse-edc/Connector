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

package org.eclipse.edc.protocol.dsp.metadata.http.api;

import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionsError;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ProtocolVersions;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Produces(APPLICATION_JSON)
@Path("/{participantContextId}/.well-known/dspace-version")
public class DspMetadataApiController {

    private final DataspaceProfileContextRegistry profileContextRegistry;
    private final TypeTransformerRegistry transformerRegistry;
    private final ParticipantContextService participantContextService;


    public DspMetadataApiController(ParticipantContextService participantContextService, DataspaceProfileContextRegistry profileContextRegistry, TypeTransformerRegistry transformerRegistry) {
        this.participantContextService = participantContextService;
        this.profileContextRegistry = profileContextRegistry;
        this.transformerRegistry = transformerRegistry;
    }

    @GET
    public Response getProtocolVersions(@PathParam("participantContextId") String participantContextId) {
        var participantContextResult = participantContextService.getParticipantContext(participantContextId);
        if (participantContextResult.failed()) {
            return notFound();
        }
        // TODO we should add if a participant context supports a certain profile
        var versions = profileContextRegistry.getProfiles().stream().map(DataspaceProfileContext::protocolVersion).distinct().toList();

        var protocolVersions = new ProtocolVersions(versions);
        var body = transformerRegistry.transform(protocolVersions, JsonObject.class);

        if (body.failed()) {
            return internalServerError(body.getFailureMessages());
        }
        return Response.status(Response.Status.OK)
                .entity(body.getContent())
                .build();
    }

    private Response badRequest(List<String> messages) {
        return errorResponse(Response.Status.BAD_REQUEST, messages);
    }

    private Response notFound() {
        return errorResponse(Response.Status.NOT_FOUND, List.of());
    }

    private Response internalServerError(List<String> messages) {
        return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, messages);
    }

    private Response errorResponse(Response.Status status, List<String> messages) {
        var error = VersionsError.Builder.newInstance().code(status.toString()).messages(messages).build();
        var body = transformerRegistry.transform(error, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));

        return Response.status(status)
                .entity(body)
                .build();
    }

}
