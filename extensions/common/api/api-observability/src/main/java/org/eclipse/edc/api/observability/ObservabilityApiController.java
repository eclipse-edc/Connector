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
 *       Fraunhofer Institute for Software and Systems Engineering - add negotiation endpoint
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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.health.HealthStatus;
import org.jetbrains.annotations.NotNull;


@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/check")
public class ObservabilityApiController implements ObservabilityApi {

    private final HealthCheckService healthCheckService;

    /**
     * This deprecation is used to permit a softer transition from the deprecated `web.http` (default) config group to the
     * current `web.http.management`
     *
     * @deprecated "web.http.management" config should be used instead of "web.http" (default)
     */
    @Deprecated(since = "milestone8")
    private final boolean deprecated;
    private final Monitor monitor;

    public ObservabilityApiController(HealthCheckService provider, boolean deprecated, Monitor monitor) {
        healthCheckService = provider;
        this.deprecated = deprecated;
        this.monitor = monitor;
    }

    @GET
    @Path("health")
    @Override
    public Response checkHealth() {
        if (deprecated) {
            monitor.warning(deprecationMessage());
        }
        var status = healthCheckService.getStartupStatus();
        return createResponse(status);
    }

    @GET
    @Path("liveness")
    @Override
    public Response getLiveness() {
        if (deprecated) {
            monitor.warning(deprecationMessage());
        }
        var status = healthCheckService.isLive();
        return createResponse(status);

    }

    @GET
    @Path("readiness")
    @Override
    public Response getReadiness() {
        if (deprecated) {
            monitor.warning(deprecationMessage());
        }
        var status = healthCheckService.isReady();
        return createResponse(status);
    }

    @GET
    @Path("startup")
    @Override
    public Response getStartup() {
        if (deprecated) {
            monitor.warning(deprecationMessage());
        }
        var status = healthCheckService.getStartupStatus();
        return createResponse(status);
    }

    @NotNull
    private String deprecationMessage() {
        return "The /check/* endpoint has been moved under the 'management' context, please update your url accordingly, because this endpoint will be deleted in the next releases";
    }

    private Response createResponse(HealthStatus status) {
        return status.isHealthy() ?
                Response.ok().entity(status).build() :
                Response.status(503).entity(status).build();
    }

}
