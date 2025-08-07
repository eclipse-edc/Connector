/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - add negotiation endpoint
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.api.observability;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.health.HealthStatus;


@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/check")
public class ObservabilityApiController implements ObservabilityApi {

    private final HealthCheckService healthCheckService;

    public ObservabilityApiController(HealthCheckService provider) {
        healthCheckService = provider;
    }

    @GET
    @Path("health")
    @Override
    public Response checkHealth() {
        var status = healthCheckService.getStartupStatus();
        return createResponse(status);
    }

    @GET
    @Path("liveness")
    @Override
    public Response getLiveness() {
        var status = healthCheckService.isLive();
        return createResponse(status);

    }

    @GET
    @Path("readiness")
    @Override
    public Response getReadiness() {
        var status = healthCheckService.isReady();
        return createResponse(status);
    }

    @GET
    @Path("startup")
    @Override
    public Response getStartup() {
        var status = healthCheckService.getStartupStatus();
        return createResponse(status);
    }

    private Response createResponse(HealthStatus status) {
        return status.isHealthy() ?
                Response.ok().entity(status).build() :
                Response.status(503).entity(status).build();
    }

}
