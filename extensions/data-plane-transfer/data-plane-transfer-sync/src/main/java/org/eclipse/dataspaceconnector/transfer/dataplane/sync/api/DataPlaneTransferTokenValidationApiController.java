/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.common.token.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;

import static java.lang.String.join;
import static org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants.DATA_ADDRESS;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/token")
public class DataPlaneTransferTokenValidationApiController {

    private final Monitor monitor;
    private final TokenValidationService service;
    private final DataEncrypter dataEncrypter;

    public DataPlaneTransferTokenValidationApiController(Monitor monitor, TokenValidationService service, DataEncrypter dataEncrypter) {
        this.monitor = monitor;
        this.service = service;
        this.dataEncrypter = dataEncrypter;
    }

    @GET
    public Response validate(@HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        var validationResult = service.validate(token);
        if (validationResult.failed()) {
            var failure = "Token validation failed: " + join(", ", validationResult.getFailureMessages());
            monitor.debug(failure);
            return Response.status(Response.Status.BAD_REQUEST).entity(failure).build();
        }

        monitor.debug("Token validation successful");

        var validToken = validationResult.getContent();

        // decrypt data address
        validToken.getClaims().computeIfPresent(DATA_ADDRESS, (s, s2) -> dataEncrypter.decrypt((String) s2));

        return Response.ok(validToken).build();
    }
}
