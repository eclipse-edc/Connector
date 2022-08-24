/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.extension.jersey.validation.integrationtest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

// note that this class MUST be public, otherwise it won't get picked up by the interceptor

@Path("/")
public class TestController {

    @GET
    @Path("greeting")
    @Produces("application/json")
    public Response greeting() {
        return Response.ok("hello world!").build();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("greeting")
    public Response greetingWithName(GreetingDto dto) {
        return Response.ok("hello, " + dto.getName()).build();
    }

}
