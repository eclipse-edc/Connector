/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *   ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.ContractNegotiationDto;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;

import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/contractnegotiations")
public class ContractNegotiationController {
    private final Monitor monitor;

    public ContractNegotiationController(Monitor monitor) {
        this.monitor = monitor;
    }

    @GET
    public List<ContractNegotiationDto> getNegotiations(@QueryParam("offset") Integer offset,
                                                        @QueryParam("limit") Integer limit,
                                                        @QueryParam("filter") String filterExpression,
                                                        @QueryParam("sort") SortOrder sortOrder,
                                                        @QueryParam("sortField") String sortField) {
        var spec = QuerySpec.Builder.newInstance()
                .offset(offset)
                .limit(limit)
                .sortField(sortField)
                .filter(filterExpression)
                .sortOrder(sortOrder)
                .build();

        monitor.debug(format("Get all contract definitions %s", spec));

        return Collections.emptyList();
    }

    @GET
    @Path("/{id}")
    public ContractNegotiationDto getNegotiation(@PathParam("id") String id) {
        monitor.debug(format("Get contract negotiation with id %s", id));
        return null;
    }

    @GET
    @Path("/{id}/state")
    public String getNegotiationState(@PathParam("id") String id) {
        monitor.debug(format("Get contract negotiation state with id %s", id));
        return "some state";
    }

    @POST
    @Path("/{id}/cancel")
    public void cancelNegotiation(@PathParam("id") String id) {
        // TODO move Negotiation to the CANCELLING/CANCELLED state
        // TODO Throw IllegalStateException if not possible
        monitor.debug(format("Attempting to cancel contract negotiation with id %s", id));
    }

    @POST
    @Path("/{id}/decline")
    public void declineNegotiation(@PathParam("id") String id) {
        // TODO move Negotiation to the DECLINING/DECLINED state
        // TODO Throw IllegalStateException if not possible
        monitor.debug(format("Attempting to decline contract negotiation with id %s", id));
    }
}
