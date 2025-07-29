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

package org.eclipse.edc.connector.dataplane.provision.http.port;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.provision.DeprovisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;

import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.mapToException;

@Path("/")
public class ProvisionHttpApiController implements ProvisionHttpApi {

    private final DataPlaneManager dataPlaneManager;

    public ProvisionHttpApiController(DataPlaneManager dataPlaneManager) {
        this.dataPlaneManager = dataPlaneManager;
    }

    @Override
    @POST
    @Path("/{flowId}/{resourceId}/provision")
    public void provision(@PathParam("flowId") String flowId, @PathParam("resourceId") String resourceId, ProvisionHttpResponse provisionHttpResponse) {
        var provisionedResource = ProvisionedResource.Builder.newInstance()
                .id(resourceId)
                .flowId(flowId)
                .dataAddress(provisionHttpResponse.dataAddress())
                .build();

        dataPlaneManager.resourceProvisioned(provisionedResource)
                .orElseThrow(it -> mapToException(it, DataFlow.class, flowId));
    }

    @Override
    @POST
    @Path("/{flowId}/{resourceId}/deprovision")
    public void deprovision(@PathParam("flowId") String flowId, @PathParam("resourceId") String resourceId) {
        var resource = DeprovisionedResource.Builder.newInstance().id(resourceId).flowId(flowId).build();

        dataPlaneManager.resourceDeprovisioned(resource)
                .orElseThrow(it -> mapToException(it, DataFlow.class, flowId));
    }
}
