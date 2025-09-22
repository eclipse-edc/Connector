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

package org.eclipse.edc.connector.controlplane.api.management.transferprocess.v3;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.BaseTransferProcessApiController;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v3/transferprocesses")
public class TransferProcessApiV3Controller extends BaseTransferProcessApiController implements TransferProcessApiV3 {

    public TransferProcessApiV3Controller(Monitor monitor, TransferProcessService service, TypeTransformerRegistry transformerRegistry,
                                          JsonObjectValidatorRegistry validatorRegistry, SingleParticipantContextSupplier participantContextSupplier) {
        super(monitor, service, transformerRegistry, validatorRegistry, participantContextSupplier);
    }

    @POST
    @Path("request")
    @Override
    public JsonArray queryTransferProcessesV3(JsonObject querySpecJson) {
        return queryTransferProcesses(querySpecJson);
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getTransferProcessV3(@PathParam("id") String id) {
        return getTransferProcess(id);
    }

    @GET
    @Path("/{id}/state")
    @Override
    public JsonObject getTransferProcessStateV3(@PathParam("id") String id) {
        return getTransferProcessState(id);
    }

    @POST
    @Override
    public JsonObject initiateTransferProcessV3(JsonObject transferRequest) {
        return initiateTransferProcess(transferRequest);
    }

    @POST
    @Path("/{id}/deprovision")
    @Override
    public void deprovisionTransferProcessV3(@PathParam("id") String id) {
        deprovisionTransferProcess(id);
    }

    @POST
    @Path("/{id}/terminate")
    @Override
    public void terminateTransferProcessV3(@PathParam("id") String id, JsonObject terminateTransfer) {
        terminateTransferProcess(id, terminateTransfer);
    }

    @POST
    @Path("/{id}/suspend")
    @Override
    public void suspendTransferProcessV3(@PathParam("id") String id, JsonObject suspendTransfer) {
        suspendTransferProcess(id, suspendTransfer);
    }

    @POST
    @Path("/{id}/resume")
    @Override
    public void resumeTransferProcessV3(@PathParam("id") String id) {
        resumeTransferProcess(id);
    }
}
