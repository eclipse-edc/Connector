/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;

import java.util.Map;

@Path("/provider")
public class ProviderBackendApiController {

    @Path("/data")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getData(@DefaultValue("some information") @QueryParam("message") String message) {
        return Map.of("message", message);
    }

    @Path("/oauth2data")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getOauth2Data(@DefaultValue("some information") @QueryParam("message") String message, @HeaderParam("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) { // TODO: improve token validation
            throw new NotAuthorizedException("The authorization token is not valid: " + authorization);
        } else {
            return Map.of("message", message);
        }
    }
}
