/*
 *  Copyright (c) 2020 - 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.api.auth;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.exception.mappers.EdcApiExceptionMapper;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class DummyApiExtension implements ServiceExtension {

    @Inject
    WebService webService;

    @Inject
    private AuthenticationService service;

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource("data", new DummyApiController());
        webService.registerResource("data", new AuthenticationRequestFilter(service));
        webService.registerResource("data", new EdcApiExceptionMapper());
    }

    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/")
    public class DummyApiController {

        @GET
        @Path("dummy")
        public String dummy() {
            return "{\"response\":\"Test signal sent!\"}";
        }
    }
}
