/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.port.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.signaling.domain.DataPlaneRegistrationMessage;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.mapToException;

@Path("/dataplanes")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class DataPlaneRegistrationApiController implements DataPlaneRegistrationApi {

    private final DataPlaneSelectorService dataPlaneSelectorService;

    public DataPlaneRegistrationApiController(DataPlaneSelectorService dataPlaneSelectorService) {
        this.dataPlaneSelectorService = dataPlaneSelectorService;
    }

    @Path("/")
    @PUT
    @Override
    public Response register(DataPlaneRegistrationMessage registration) {
        var dataPlaneInstance = DataPlaneInstance.Builder.newInstance()
                .id(registration.dataplaneId())
                .url(registration.endpoint())
                .allowedTransferType(registration.transferTypes())
                .build();

        dataPlaneSelectorService.register(dataPlaneInstance)
                .orElseThrow(it -> mapToException(it, DataPlaneInstance.class, registration.dataplaneId()));

        return Response.ok().build();
    }

    @Path("/{dataplaneId}")
    @DELETE
    @Override
    public Response delete(@PathParam("dataplaneId") String dataplaneId) {
        dataPlaneSelectorService.delete(dataplaneId)
                .orElseThrow(it -> mapToException(it, DataPlaneInstance.class, dataplaneId));

        return Response.ok().build();
    }

}
