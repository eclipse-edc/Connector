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
import org.eclipse.edc.protocol.dsp.spi.controlplane.service.TransferProcessService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;

import static org.eclipse.edc.protocol.dsp.util.JsonLdUtil.expandDocument;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/transfer-processes")
public class TransferProcessController {

    private Monitor monitor;

    private TransferProcessService transferProcessService;

    private ObjectMapper mapper;

    public TransferProcessController(Monitor monitor, TransferProcessService transferProcessService, TypeManager typeManager) {
        this.monitor = monitor;
        this.transferProcessService = transferProcessService;
        this.mapper = typeManager.getMapper("json-ld"); //TODO Use correct mapper
    }

    //Provider side
    @GET
    @Path("/{id}")
    public JsonObject getTransferProcess(@PathParam("id") String id) {

        return transferProcessService.getTransferProcessByID(id);
    }

    //Provider side
    @POST
    @Path("/request")
    public JsonObject initiateTransferProcess(JsonObject jsonObject) {
        var document = expandDocument(jsonObject).getJsonObject(0);

        return transferProcessService.initiateTransferProcess(document);
    }

    //both sides
    @POST
    @Path("/{id}/start")
    public void consumerTransferProcessStart(@PathParam("id") String id, JsonObject jsonObject) {
        var document = expandDocument(jsonObject).getJsonObject(0);

        transferProcessService.transferProcessStart(id, document);
    }

    //both sides
    @POST
    @Path("/{id}/completion")
    public void consumerTransferProcessCompletion(@PathParam("id") String id, JsonObject jsonObject) {
        var document = expandDocument(jsonObject).getJsonObject(0);

        transferProcessService.transferProcessCompletion(id,document);
    }

    //both sides
    @POST
    @Path("/{id}/termination")
    public void consumerTransferProcessTermination(@PathParam("id") String id, JsonObject jsonObject) {
        var document = expandDocument(jsonObject).getJsonObject(0);

        transferProcessService.transferProcessTermination(id, document);
    }

    //both sides
    @POST
    @Path("/{id}/suspension")
    public void consumerTransferProcessSuspension(@PathParam("id") String id, JsonObject jsonObject) {
        var document = expandDocument(jsonObject).getJsonObject(0);

        transferProcessService.transferProcessSuspension(id, document);
    }
}
