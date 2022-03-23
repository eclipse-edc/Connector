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

package org.eclipse.dataspaceconnector.dataplane.selector.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.dataplane.selector.DataPlaneSelectorService;
import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;

import java.util.List;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/instances")
public class DataplaneSelectorApiController {

    private final DataPlaneSelectorService selectionService;

    public DataplaneSelectorApiController(DataPlaneSelectorService selectionService) {
        this.selectionService = selectionService;
    }

    @POST
    @Path("select")
    public DataPlaneInstance find(SelectionRequest request) {
        if (request.getStrategy() != null) {
            return selectionService.select(request.getSource(), request.getDestination(), request.getStrategy());
        } else {
            return selectionService.select(request.getSource(), request.getDestination());
        }
    }


    @POST
    public void addEntry(DataPlaneInstance instance) {
        selectionService.addInstance(instance);
    }

    @GET
    public List<DataPlaneInstance> getAll() {
        return selectionService.getAll();
    }
}
