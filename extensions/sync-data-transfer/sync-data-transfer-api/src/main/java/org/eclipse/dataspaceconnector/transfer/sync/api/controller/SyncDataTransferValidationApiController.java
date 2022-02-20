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

package org.eclipse.dataspaceconnector.transfer.sync.api.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.spi.iam.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

@Path("/validation")
public class SyncDataTransferValidationApiController {

    private final Monitor monitor;
    private final TokenValidationService tokenValidationService;

    public SyncDataTransferValidationApiController(Monitor monitor, TokenValidationService tokenValidationService) {
        this.monitor = monitor;
        this.tokenValidationService = tokenValidationService;
    }

    @GET
    public Response validate(@HeaderParam("Authorization") String token) {
        monitor.info("Received request for token validation");
        var result = tokenValidationService.validate(token);
        if (result.failed()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Token validation failed: " + String.join(", ", result.getFailureMessages()))
                    .build();
        }
        return Response.ok(result.getContent()).build();
    }
}
