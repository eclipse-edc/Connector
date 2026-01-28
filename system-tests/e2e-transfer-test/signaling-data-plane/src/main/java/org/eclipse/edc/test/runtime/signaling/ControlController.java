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

package org.eclipse.edc.test.runtime.signaling;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.dataplane.Dataplane;
import org.eclipse.dataplane.domain.DataAddress;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.edc.spi.monitor.Monitor;

import static java.util.Collections.emptyList;

@Path("/control")
public class ControlController {

    private final Monitor monitor;
    private final Dataplane dataplane;
    private final SignalingDataPlaneRuntimeExtension.ApiConfiguration apiConfiguration;

    public ControlController(Monitor monitor, Dataplane dataplane, SignalingDataPlaneRuntimeExtension.ApiConfiguration apiConfiguration) {
        this.monitor = monitor;
        this.dataplane = dataplane;
        this.apiConfiguration = apiConfiguration;
    }

    @POST
    @Path("/flows/{flowId}/complete-preparation")
    public void completePreparation(@PathParam("flowId") String flowId) {
        dataplane.notifyPrepared(flowId, dataFlow -> {
            dataFlow.setDataAddress(new DataAddress("http", apiConfiguration.receiveDataEndpoint(), emptyList()));
            return Result.success(dataFlow);
        }).orElseThrow(RuntimeException::new);
    }

    @POST
    @Path("/flows/{flowId}/complete-startup")
    public void completeStartup(@PathParam("flowId") String flowId) {
        dataplane.notifyStarted(flowId, dataFlow -> {
            dataFlow.setDataAddress(new DataAddress("http", apiConfiguration.dataSourceEndpoint(), emptyList()));
            return Result.success(dataFlow);
        }).orElseThrow(RuntimeException::new);
    }

    @GET
    @Path("/source")
    public String dataSource() {
        monitor.info("Data requested");
        return "data";
    }

}
