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
 *
 */

package org.eclipse.edc.connector.dataplane.api.controller.v1;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.MediaType;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/v1/dataflows")
public class DataPlaneSignalingApiController implements DataPlaneSignalingApi {

    @POST
    @Override
    public JsonObject start(JsonObject dataFlowStartMessage, AsyncResponse response) {
        throw new UnsupportedOperationException();
    }

    @GET
    @Path("/{id}/state")
    @Override
    public JsonObject getTransferState(@PathParam("id") String transferProcessId) {
        throw new UnsupportedOperationException();
    }

    @POST
    @Path("/{id}/terminate")
    @Override
    public void terminate(@PathParam("id") String transferProcessId, JsonObject terminationMessage) {
        throw new UnsupportedOperationException();
    }

    @POST
    @Path("/{id}/suspend")
    @Override
    public void suspend(@PathParam("id") String id, JsonObject suspendMessage) {
        throw new UnsupportedOperationException();
    }
}
