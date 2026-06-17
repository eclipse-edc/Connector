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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionsError;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ParticipantProfileService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.List;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_BINDING;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_PATH;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_PROTOCOL_VERSIONS;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_VERSION;

@Produces(APPLICATION_JSON)
@Path("/{participantContextId}/.well-known/dspace-version")
public class DspVirtualMetadataApiController {

    private final ParticipantProfileService profileResolver;
    private final TypeTransformerRegistry transformerRegistry;
    private final ParticipantContextService participantContextService;


    public DspVirtualMetadataApiController(ParticipantContextService participantContextService, ParticipantProfileService profileResolver, TypeTransformerRegistry transformerRegistry) {
        this.participantContextService = participantContextService;
        this.profileResolver = profileResolver;
        this.transformerRegistry = transformerRegistry;
    }

    @GET
    public Response getProtocolVersions(@PathParam("participantContextId") String participantContextId) {
        var participantContextResult = participantContextService.getParticipantContext(participantContextId);
        if (participantContextResult.failed()) {
            return notFound();
        }

        // Advertise only the DSP versions of profiles this participant is associated with.
        // A participant with no associated profiles returns an empty version list.
        var versions = profileResolver.resolveAll(participantContextId).stream()
                .map(this::mapToProtocolVersion)
                .distinct()
                .collect(toJsonArray());

        return Response.status(Response.Status.OK)
                .entity(Json.createObjectBuilder().add(DSPACE_PROPERTY_PROTOCOL_VERSIONS, versions).build())
                .build();
    }
    
    private Response notFound() {
        return errorResponse(Response.Status.NOT_FOUND, List.of());
    }

    private Response errorResponse(Response.Status status, List<String> messages) {
        var error = VersionsError.Builder.newInstance().code(status.toString()).messages(messages).build();
        var body = transformerRegistry.transform(error, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));

        return Response.status(status)
                .entity(body)
                .build();
    }

    private JsonObject mapToProtocolVersion(DataspaceProfileContext context) {
        var protocolVersion = context.protocolVersion();
        return Json.createObjectBuilder()
                .add(DSPACE_PROPERTY_VERSION, protocolVersion.version())
                .add(DSPACE_PROPERTY_PATH, protocolVersion.path())
                .add(DSPACE_PROPERTY_BINDING, protocolVersion.binding())
                .add("profile", context.name())
                .build();
    }

}
