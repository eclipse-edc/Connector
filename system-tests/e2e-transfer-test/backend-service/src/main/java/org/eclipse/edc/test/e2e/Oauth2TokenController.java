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

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.util.Map;

import static java.lang.String.format;

@Path("/oauth2")
public class Oauth2TokenController {

    private final Monitor monitor;

    public Oauth2TokenController(Monitor monitor) {
        this.monitor = monitor;
    }

    @POST
    @Path("/token")
    public Map<String, String> getToken(@FormParam("client_id") String clientId, @FormParam("client_secret") String clientSecret) {
        monitor.info("Oauth2 token requested");
        if (!"clientId".equals(clientId) || !"clientSecret".equals(clientSecret)) {
            var message = format("Cannot validate token request, parameters are not valid: client_id %s - client_secret %s", clientId, clientSecret);
            monitor.severe(message);
            throw new InvalidRequestException(message);
        }
        return Map.of("access_token", "atoken"); // TODO: there's a way to easily provide an actual jwt token?
    }
}
