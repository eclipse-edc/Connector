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
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.policy;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionDto;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;

import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/policies")
public class PolicyApiController {
    private final Monitor monitor;

    public PolicyApiController(Monitor monitor) {
        this.monitor = monitor;
    }

    @GET
    public List<PolicyDefinitionDto> getAllPolicies(@QueryParam("offset") Integer offset,
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
        monitor.debug(format("get all policys %s", spec));

        return Collections.emptyList();

    }

    @GET
    @Path("{id}")
    public PolicyDefinitionDto getPolicy(@PathParam("id") String id) {
        monitor.debug(format("get policy with ID %s", id));

        return null;
    }

    @POST
    public void createPolicy(PolicyDefinitionDto dto) {
        monitor.debug("create new policy");
    }

    @DELETE
    @Path("{id}")
    public void deletePolicy(@PathParam("id") String id) {
        monitor.debug(format("Attempting to delete policy with id %s", id));
    }


}
