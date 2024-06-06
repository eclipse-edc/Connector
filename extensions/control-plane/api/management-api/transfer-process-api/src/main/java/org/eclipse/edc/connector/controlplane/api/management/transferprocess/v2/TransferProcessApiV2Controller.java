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

package org.eclipse.edc.connector.controlplane.api.management.transferprocess.v2;

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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.api.ApiWarnings.deprecationWarning;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v2/transferprocesses")
public class TransferProcessApiV2Controller extends BaseTransferProcessApiController implements TransferProcessApiV2 {
    public TransferProcessApiV2Controller(Monitor monitor, TransferProcessService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry) {
        super(monitor, service, transformerRegistry, validatorRegistry);
    }

    @POST
    @Path("request")
    @Override
    public JsonArray queryTransferProcessesV2(JsonObject querySpecJson) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return queryTransferProcesses(querySpecJson);
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getTransferProcessV2(@PathParam("id") String id) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return getTransferProcess(id);
    }

    @GET
    @Path("/{id}/state")
    @Override
    public JsonObject getTransferProcessStateV2(@PathParam("id") String id) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return getTransferProcessState(id);
    }

    @POST
    @Override
    public JsonObject initiateTransferProcessV2(JsonObject transferRequest) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return initiateTransferProcess(transferRequest);
    }

    @POST
    @Path("/{id}/deprovision")
    @Override
    public void deprovisionTransferProcessV2(@PathParam("id") String id) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        deprovisionTransferProcess(id);
    }

    @POST
    @Path("/{id}/terminate")
    @Override
    public void terminateTransferProcessV2(@PathParam("id") String id, JsonObject terminateTransfer) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        terminateTransferProcess(id, terminateTransfer);
    }

    @POST
    @Path("/{id}/suspend")
    @Override
    public void suspendTransferProcessV2(@PathParam("id") String id, JsonObject suspendTransfer) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        suspendTransferProcess(id, suspendTransfer);
    }

    @POST
    @Path("/{id}/resume")
    @Override
    public void resumeTransferProcessV2(@PathParam("id") String id) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        resumeTransferProcess(id);
    }
}
