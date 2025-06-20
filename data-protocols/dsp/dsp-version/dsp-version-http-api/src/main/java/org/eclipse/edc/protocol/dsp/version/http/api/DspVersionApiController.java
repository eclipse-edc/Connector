/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Cofinity-X - unauthenticated DSP version endpoint
 *
 */

package org.eclipse.edc.protocol.dsp.version.http.api;

import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionProtocolService;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionsError;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Produces(APPLICATION_JSON)
@Path("/.well-known/dspace-version")
public class DspVersionApiController {

    private final VersionProtocolService service;
    private final TypeTransformerRegistry transformerRegistry;

    public DspVersionApiController(VersionProtocolService service, TypeTransformerRegistry transformerRegistry) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
    }

    @GET
    public Response getProtocolVersions() {
        var result = service.getAll();
        if (result.failed()) {
            return badRequest(result.getFailureMessages());
        }
        var protocolVersions = result.getContent();
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
