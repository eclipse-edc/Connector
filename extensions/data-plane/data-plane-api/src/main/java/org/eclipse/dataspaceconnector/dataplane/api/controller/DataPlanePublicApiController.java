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
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.dataplane.spi.api.TokenValidationClient;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSink;
import org.eclipse.dataspaceconnector.spi.exception.NotAuthorizedException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.eclipse.dataspaceconnector.dataplane.api.response.ResponseFunctions.internalErrors;
import static org.eclipse.dataspaceconnector.dataplane.api.response.ResponseFunctions.validationError;

@Path("{any:.*}")
@Produces(MediaType.APPLICATION_JSON)
public class DataPlanePublicApiController implements DataPlanePublicApi {

    private final DataPlaneManager dataPlaneManager;
    private final TokenValidationClient tokenValidationClient;
    private final DataFlowRequestSupplier requestSupplier;
    private final Monitor monitor;
    private final ExecutorService executorService;

    public DataPlanePublicApiController(DataPlaneManager dataPlaneManager,
                                        TokenValidationClient tokenValidationClient,
                                        Monitor monitor,
                                        ExecutorService executorService) {
        this.dataPlaneManager = dataPlaneManager;
        this.tokenValidationClient = tokenValidationClient;
        this.requestSupplier = new DataFlowRequestSupplier();
        this.monitor = monitor;
        this.executorService = executorService;
    }

    @GET
    @Override
    public void get(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    /**
     * Sends a {@link DELETE} request to the data source and returns data.
     *
     * @param requestContext Request context.
     * @param response       Data fetched from the data source.
     */
    @DELETE
    @Override
    public void delete(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    /**
     * Sends a {@link PATCH} request to the data source and returns data.
     *
     * @param requestContext Request context.
     * @param response       Data fetched from the data source.
     */
    @PATCH
    @Override
    public void patch(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    /**
     * Sends a {@link PUT} request to the data source and returns data.
     *
     * @param requestContext Request context.
     * @param response       Data fetched from the data source.
     */
    @PUT
    @Override
    public void put(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    /**
     * Sends a {@link POST} request to the data source and returns data.
     *
     * @param requestContext Request context.
     * @param response       Data fetched from the data source.
     */
    @POST
    @Override
    public void post(@Context ContainerRequestContext requestContext, @Suspended AsyncResponse response) {
        handle(requestContext, response);
    }

    private void handle(ContainerRequestContext context, AsyncResponse response) {
        var contextApi = new ContainerRequestContextApiImpl(context);
        var token = contextApi.headers().get(HttpHeaders.AUTHORIZATION);
        if (token == null) {
            response.resume(validationError("Missing bearer token"));
            return;
        }

        var dataAddress = extractSourceDataAddress(token);
        var dataFlowRequest = requestSupplier.apply(contextApi, dataAddress);

        var validationResult = dataPlaneManager.validate(dataFlowRequest);
        if (validationResult.failed()) {
            var errorMsg = validationResult.getFailureMessages().isEmpty() ?
                    format("Failed to validate request with id: %s", dataFlowRequest.getId()) :
                    join(",", validationResult.getFailureMessages());
            response.resume(validationError(errorMsg));
            return;
        }

        var stream = new ByteArrayOutputStream();
        var sink = new OutputStreamDataSink(stream, executorService, monitor);

        dataPlaneManager.transfer(sink, dataFlowRequest)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        if (result.succeeded()) {
                            response.resume(Response.ok(stream.toString()).build());
                        } else {
                            response.resume(internalErrors(result.getFailureMessages()));
                        }
                    } else {
                        response.resume(internalErrors(List.of("Unhandled exception occurred during data transfer: " + throwable.getMessage())));
                    }
                });
    }

    /**
     * Invoke the {@link TokenValidationClient} with the provided token to retrieve the source data address.
     *
     * @param token input token
     * @return the source {@link DataAddress}.
     * @throws NotAuthorizedException if {@link TokenValidationClient} invokation failed.
     */
    private DataAddress extractSourceDataAddress(String token) {
        var result = tokenValidationClient.call(token);
        if (result.failed()) {
            throw new NotAuthorizedException(String.join(", ", result.getFailureMessages()));
        }
        return result.getContent();
    }
}
