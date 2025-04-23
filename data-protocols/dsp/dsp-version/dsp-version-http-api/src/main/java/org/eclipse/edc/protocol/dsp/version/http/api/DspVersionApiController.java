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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersions;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionProtocolService;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionsError;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.http.spi.message.GetDspRequest;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;

@Produces(APPLICATION_JSON)
@Path("/.well-known/dspace-version")
public class DspVersionApiController {

    private final DspRequestHandler requestHandler;
    private final VersionProtocolService service;

    public DspVersionApiController(DspRequestHandler requestHandler, VersionProtocolService service) {
        this.requestHandler = requestHandler;
        this.service = service;
    }
    
    @GET
    public Response getProtocolVersions() {
        var request = GetDspRequest.Builder.newInstance(ProtocolVersions.class, VersionsError.class)
                .token(null)
                .authRequired(false)
                .serviceCall((id, tokenRepresentation) -> service.getAll())
                .protocol(DATASPACE_PROTOCOL_HTTP)
                .errorProvider(VersionsError.Builder::newInstance)
                .build();

        return requestHandler.getResource(request);
    }

}
