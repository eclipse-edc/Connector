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

package org.eclipse.edc.protocol.dsp.controlplane.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.protocol.dsp.spi.controlplane.service.DspTransferProcessService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.JsonLdUtil.expandDocument;


@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/transfers")
public class TransferProcessController {

    private Monitor monitor;

    private DspTransferProcessService dspTransferProcessService;

    private ObjectMapper mapper;

    public TransferProcessController(Monitor monitor, DspTransferProcessService dspTransferProcessService, TypeManager typeManager) {
        this.monitor = monitor;
        this.dspTransferProcessService = dspTransferProcessService;
        this.mapper = typeManager.getMapper("json-ld");
    }

    //Provider side
    @GET
    @Path("/{id}")
    public JsonObject getTransferProcess(@PathParam("id") String id) {
        monitor.debug(format("DSP: Incoming request for transfer process with id %s", id));

        return dspTransferProcessService.getTransferProcessByID(id);
    }

    //Provider side
    @POST
    @Path("/request")
    public JsonObject initiateTransferProcess(JsonObject jsonObject) {
        monitor.debug("DSP: Incoming TransferRequestMessage for initiating a transfer process");

        var document = expandDocument(jsonObject).getJsonObject(0);
        var result = dspTransferProcessService.initiateTransferProcess(document);

        return result;
    }

    //both sides
    @POST
    @Path("/{id}/start")
    public void consumerTransferProcessStart(@PathParam("id") String id, JsonObject jsonObject) {
        monitor.debug(format("DSP: Incoming TransferStartMessage for transfer process %s"));
        var document = expandDocument(jsonObject).getJsonObject(0);

        dspTransferProcessService.transferProcessStart(id, document);
    }

    //both sides
    @POST
    @Path("/{id}/completion")
    public void consumerTransferProcessCompletion(@PathParam("id") String id, JsonObject jsonObject) {
        monitor.debug(format("DSP: Incoming TransferCompletionMessage for transfer process %s"));
        var document = expandDocument(jsonObject).getJsonObject(0);

        dspTransferProcessService.transferProcessCompletion(id,document);
    }

    //both sides
    @POST
    @Path("/{id}/termination")
    public void consumerTransferProcessTermination(@PathParam("id") String id, JsonObject jsonObject) {
        monitor.debug(format("DSP: Incoming TransferTerminationMessage for transfer process %s"));

        var document = expandDocument(jsonObject).getJsonObject(0);

        dspTransferProcessService.transferProcessTermination(id, document);
    }

    //both sides
    @POST
    @Path("/{id}/suspension")
    public void consumerTransferProcessSuspension(@PathParam("id") String id, JsonObject jsonObject) {
        monitor.debug(format("DSP: Incoming TransferSuspensionMessage for transfer process %s"));

        var document = expandDocument(jsonObject).getJsonObject(0);
        dspTransferProcessService.transferProcessSuspension(id, document);
    }
}
