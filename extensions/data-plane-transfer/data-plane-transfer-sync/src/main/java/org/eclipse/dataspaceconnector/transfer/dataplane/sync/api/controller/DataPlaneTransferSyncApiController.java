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

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.common.token.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;

import static org.eclipse.dataspaceconnector.spi.types.domain.dataplane.DataPlaneConstants.DATA_ADDRESS;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/validation")
public class DataPlaneTransferSyncApiController {

    private final Monitor monitor;
    private final TokenValidationService tokenValidationService;
    private final DataEncrypter dataEncrypter;

    public DataPlaneTransferSyncApiController(Monitor monitor, TokenValidationService tokenValidationService, DataEncrypter dataEncrypter) {
        this.monitor = monitor;
        this.tokenValidationService = tokenValidationService;
        this.dataEncrypter = dataEncrypter;
    }

    @GET
    public Response validate(@HeaderParam("Authorization") String token) {
        monitor.info("Received request for validToken validation");
        var validationResult = tokenValidationService.validate(token);
        if (validationResult.failed()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Token validation failed: " + String.join(", ", validationResult.getFailureMessages()))
                    .build();
        }

        var validToken = validationResult.getContent();

        // decrypt data address
        validToken.getClaims().computeIfPresent(DATA_ADDRESS, (s, s2) -> dataEncrypter.decrypt(s2));

        return Response.ok(validToken).build();
    }
}
