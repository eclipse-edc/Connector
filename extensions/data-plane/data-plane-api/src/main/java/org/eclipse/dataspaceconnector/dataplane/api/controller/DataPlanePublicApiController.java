/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.dataplane.api.controller;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.common.token.TokenValidationService;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSink;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.eclipse.dataspaceconnector.dataplane.api.response.ResponseFunctions.internalErrors;
import static org.eclipse.dataspaceconnector.dataplane.api.response.ResponseFunctions.notAuthorizedErrors;
import static org.eclipse.dataspaceconnector.dataplane.api.response.ResponseFunctions.success;
import static org.eclipse.dataspaceconnector.dataplane.api.response.ResponseFunctions.validationError;
import static org.eclipse.dataspaceconnector.dataplane.api.response.ResponseFunctions.validationErrors;

/**
 * Controller exposing the public endpoint of the Data Plane used to query data (synchronous data pull).
 */
@Path("{any:.*}")
@Produces(MediaType.APPLICATION_JSON)
public class DataPlanePublicApiController {

    private final DataPlaneManager dataPlaneManager;
    private final TokenValidationService tokenValidationService;
    private final Monitor monitor;
    private final DataFlowRequestFactory factory;
    private final ExecutorService executorService;

    public DataPlanePublicApiController(DataPlaneManager dataPlaneManager,
                                        TokenValidationService tokenValidationService,
                                        Monitor monitor,
                                        TypeManager typeManager,
                                        ExecutorService executorService) {
        this.dataPlaneManager = dataPlaneManager;
        this.tokenValidationService = tokenValidationService;
        this.monitor = monitor;
        this.factory = new DataFlowRequestFactory(typeManager);
        this.executorService = executorService;
    }

    /**
     * Sends a {@link GET} request to the data source and returns data.
     */
    @GET
    public void get(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    /**
     * Sends a {@link DELETE} request to the data source.
     */
    @DELETE
    public void delete(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    /**
     * Sends a {@link PATCH} request to the data source.
     */
    @PATCH
    public void patch(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    /**
     * Sends a {@link PUT} request to the data source.
     */
    @PUT
    public void put(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    /**
     * Sends a {@link POST} request to the data source.
     */
    @POST
    public void post(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    private void handle(ContainerRequestContext context, AsyncResponse response) {
        var contextApi = new ContainerRequestContextApiImpl(context);
        var bearerToken = contextApi.headers().get(HttpHeaders.AUTHORIZATION);
        if (bearerToken == null) {
            response.resume(notAuthorizedErrors(List.of("Missing bearer token")));
            return;
        }

        var tokenValidationResult = tokenValidationService.validate(bearerToken);
        if (tokenValidationResult.failed()) {
            response.resume(notAuthorizedErrors(tokenValidationResult.getFailureMessages()));
            return;
        }

        var dataFlowRequest = factory.from(contextApi, tokenValidationResult.getContent());
        var validationResult = dataPlaneManager.validate(dataFlowRequest);
        if (validationResult.failed()) {
            var res = validationResult.getFailureMessages().isEmpty()
                    ? validationError(String.format("Failed to validate request with id: %s", dataFlowRequest.getId()))
                    : validationErrors(validationResult.getFailureMessages());
            response.resume(res);
            return;
        }

        var stream = new ByteArrayOutputStream();
        var sink = new OutputStreamDataSink(stream, executorService, monitor);

        dataPlaneManager.transfer(sink, dataFlowRequest)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        if (result.succeeded()) {
                            response.resume(success(stream.toString()));
                        } else {
                            response.resume(internalErrors(result.getFailureMessages()));
                        }
                    } else {
                        response.resume(internalErrors(List.of("Unhandled exception: " + throwable.getLocalizedMessage())));
                    }
                });
    }
}
