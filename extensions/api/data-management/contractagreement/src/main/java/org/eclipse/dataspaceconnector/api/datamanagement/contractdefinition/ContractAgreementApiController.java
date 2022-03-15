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

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractAgreementDto;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;

import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/contractagreements")
public class ContractAgreementApiController {
    private final Monitor monitor;

    public ContractAgreementApiController(Monitor monitor) {
        this.monitor = monitor;
    }

    @GET
    public List<ContractAgreementDto> getAllAgreements(@QueryParam("offset") Integer offset,
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
        monitor.debug(format("get all contract definitions %s", spec));

        return Collections.emptyList();

    }

    @GET
    @Path("{id}")
    public ContractAgreementDto getContractAgreement(@PathParam("id") String id) {
        monitor.debug(format("get contract definition with ID %s", id));

        return null;
    }

}
