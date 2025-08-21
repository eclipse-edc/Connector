/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.transferprocess.v4;

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
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.connector.controlplane.api.management.transferprocess.model.SuspendTransfer.SUSPEND_TRANSFER_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.api.management.transferprocess.model.TerminateTransfer.TERMINATE_TRANSFER_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TYPE_TERM;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4alpha/transferprocesses")
public class TransferProcessApiV4Controller extends BaseTransferProcessApiController implements TransferProcessApiV4 {

    public TransferProcessApiV4Controller(Monitor monitor, TransferProcessService service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry) {
        super(monitor, service, transformerRegistry, validatorRegistry);
    }

    @POST
    @Path("request")
    @Override
    public JsonArray queryTransferProcessesV4(@SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson) {
        return queryTransferProcesses(querySpecJson);
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getTransferProcessV4(@PathParam("id") String id) {
        return getTransferProcess(id);
    }

    @GET
    @Path("/{id}/state")
    @Override
    public JsonObject getTransferProcessStateV4(@PathParam("id") String id) {
        return getTransferProcessState(id);
    }

    @POST
    @Override
    public JsonObject initiateTransferProcessV4(@SchemaType(TRANSFER_REQUEST_TYPE_TERM) JsonObject transferRequest) {
        return initiateTransferProcess(transferRequest);
    }

    @POST
    @Path("/{id}/deprovision")
    @Override
    public void deprovisionTransferProcessV4(@PathParam("id") String id) {
        deprovisionTransferProcess(id);
    }

    @POST
    @Path("/{id}/terminate")
    @Override
    public void terminateTransferProcessV4(@PathParam("id") String id, @SchemaType(TERMINATE_TRANSFER_TYPE_TERM) JsonObject terminateTransfer) {
        terminateTransferProcess(id, terminateTransfer);
    }

    @POST
    @Path("/{id}/suspend")
    @Override
    public void suspendTransferProcessV4(@PathParam("id") String id, @SchemaType(SUSPEND_TRANSFER_TYPE_TERM) JsonObject suspendTransfer) {
        suspendTransferProcess(id, suspendTransfer);
    }

    @POST
    @Path("/{id}/resume")
    @Override
    public void resumeTransferProcessV4(@PathParam("id") String id) {
        resumeTransferProcess(id);
    }
}
