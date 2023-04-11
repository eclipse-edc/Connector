/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;

import static java.lang.String.format;


@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/transfers")
public class DspTransferProcessApiController {

    private Monitor monitor;

    private ObjectMapper mapper;

    public DspTransferProcessApiController(Monitor monitor, TypeManager typeManager) {
        this.monitor = monitor;
        this.mapper = typeManager.getMapper("json-ld");
    }

    //Provider side
    @GET
    @Path("/{id}")
    public JsonObject getTransferProcess(@PathParam("id") String id) {
        monitor.debug(format("DSP: Incoming request for transfer process with id %s", id));



        return null;
    }

    //Provider side
    @POST
    @Path("/request")
    public JsonObject initiateTransferProcess(JsonObject jsonObject) {
        monitor.debug("DSP: Incoming TransferRequestMessage for initiating a transfer process");

        return null;
    }

    //both sides
    @POST
    @Path("/{id}/start")
    public void consumerTransferProcessStart(@PathParam("id") String id, JsonObject jsonObject) {
        monitor.debug(format("DSP: Incoming TransferStartMessage for transfer process %s"));
    }

    //both sides
    @POST
    @Path("/{id}/completion")
    public void consumerTransferProcessCompletion(@PathParam("id") String id, JsonObject jsonObject) {
        monitor.debug(format("DSP: Incoming TransferCompletionMessage for transfer process %s"));
    }

    //both sides
    @POST
    @Path("/{id}/termination")
    public void consumerTransferProcessTermination(@PathParam("id") String id, JsonObject jsonObject) {
        monitor.debug(format("DSP: Incoming TransferTerminationMessage for transfer process %s"));
    }

    //both sides
    @POST
    @Path("/{id}/suspension")
    public void consumerTransferProcessSuspension(@PathParam("id") String id, JsonObject jsonObject) {
        monitor.debug(format("DSP: Incoming TransferSuspensionMessage for transfer process %s"));
    }
}