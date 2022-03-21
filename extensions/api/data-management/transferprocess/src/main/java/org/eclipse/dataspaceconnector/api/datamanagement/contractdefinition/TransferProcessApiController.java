/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;

import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/transferprocess")
public class TransferProcessApiController implements TransferProcessApi {
    private final Monitor monitor;

    public TransferProcessApiController(Monitor monitor) {
        this.monitor = monitor;
    }

    @GET
    @Override
    public List<TransferProcessDto> getAllTransferProcesses(@QueryParam("offset") Integer offset,
                                                            @QueryParam("limit") Integer limit,
                                                            @QueryParam("filter") String filterExpression,
                                                            @QueryParam("sort") SortOrder sortOrder,
                                                            @QueryParam("sortField") String sortField) {
        var spec = QuerySpec.Builder.newInstance()
                .offset(offset)
                .limit(limit)
                .sortField(sortField)
                .filter(filterExpression)
                .sortOrder(sortOrder).build();
        monitor.debug(format("get all TransferProcesses %s", spec));

        return Collections.emptyList();

    }

    @GET
    @Path("{id}")
    @Override
    public TransferProcessDto getTransferProcess(@PathParam("id") String id) {
        monitor.debug(format("get TransferProcess with ID %s", id));

        return null;
    }

    @GET
    @Path("{id}/state")
    @Override
    public String getTransferProcessState(@PathParam("id") String id) {
        monitor.debug(format("get TransferProcess State with ID %s", id));

        return "";
    }

    @POST
    @Path("{id}/cancel")
    @Override
    public void cancelTransferProcess(@PathParam("id") String id) {
        monitor.debug("Cancelling TransferProcess with ID " + id);
    }

    @POST
    @Path("{id}/deprovision")
    @Override
    public void deprovisionTransferProcess(@PathParam("id") String id) {
        monitor.debug(format("Attempting to deprovision TransferProcess with id %s", id));
    }
}
