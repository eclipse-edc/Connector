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
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.common.token.TokenValidationService;
import org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSinkFactory;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final ContainerRequestContextApi requestContextApi;
    private final TypeManager typeManager;
    private final ExecutorService executorService;

    public DataPlanePublicApiController(DataPlaneManager dataPlaneManager,
                                        TokenValidationService tokenValidationService,
                                        Monitor monitor,
                                        ContainerRequestContextApi wrapper,
                                        TypeManager typeManager,
                                        ExecutorService executorService) {
        this.dataPlaneManager = dataPlaneManager;
        this.tokenValidationService = tokenValidationService;
        this.monitor = monitor;
        this.requestContextApi = wrapper;
        this.typeManager = typeManager;
        this.executorService = executorService;
    }

    /**
     * Sends a {@link GET} request to the data source and returns data.
     */
    @GET
    public Response get(@Context ContainerRequestContext requestContext) {
        return handle(requestContext);
    }

    /**
     * Sends a {@link DELETE} request to the data source.
     */
    @DELETE
    public Response delete(@Context ContainerRequestContext requestContext) {
        return handle(requestContext);
    }

    /**
     * Sends a {@link PATCH} request to the data source.
     */
    @PATCH
    public Response patch(@Context ContainerRequestContext requestContext) {
        return handle(requestContext);
    }

    /**
     * Sends a {@link PUT} request to the data source.
     */
    @PUT
    public Response put(@Context ContainerRequestContext requestContext) {
        return handle(requestContext);
    }

    /**
     * Sends a {@link POST} request to the data source.
     */
    @POST
    public Response post(@Context ContainerRequestContext requestContext) {
        return handle(requestContext);
    }

    private Response handle(ContainerRequestContext requestContext) {
        var bearerToken = requestContextApi.authHeader(requestContext);
        if (bearerToken == null) {
            return notAuthorizedErrors(List.of("Missing bearer token"));
        }

        // validate and decode input token
        var tokenValidationResult = tokenValidationService.validate(bearerToken);
        if (tokenValidationResult.failed()) {
            return notAuthorizedErrors(tokenValidationResult.getFailureMessages());
        }

        var properties = requestContextApi.properties(requestContext);
        var dataFlowRequest = createDataFlowRequest(tokenValidationResult.getContent(), properties);

        var validationResult = dataPlaneManager.validate(dataFlowRequest);
        if (validationResult.failed()) {
            return validationResult.getFailureMessages().isEmpty() ?
                    validationError(String.format("Failed to validate request with id: %s", dataFlowRequest.getId())) :
                    validationErrors(validationResult.getFailureMessages());
        }

        // perform the data transfer
        var stream = new ByteArrayOutputStream();
        var sink = new OutputStreamDataSink(stream, executorService, monitor);
        var transferResult = dataPlaneManager.transfer(sink, dataFlowRequest)
                .exceptionally(throwable -> StatusResult.failure(ResponseStatus.FATAL_ERROR, "Unhandled exception: " + throwable.getMessage()))
                .join();
        if (transferResult.failed()) {
            return internalErrors(transferResult.getFailureMessages());
        }

        return success(stream.toString());
    }


    /**
     * Create a {@link DataFlowRequest} based on the decoded claim token and the request content.
     */
    private DataFlowRequest createDataFlowRequest(ClaimToken claims, Map<String, String> properties) {
        var dataAddress = typeManager.readValue(claims.getClaims().get(DataPlaneConstants.DATA_ADDRESS), DataAddress.class);
        return DataFlowRequest.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(dataAddress)
                .destinationDataAddress(DataAddress.Builder.newInstance()
                        .type(OutputStreamDataSinkFactory.TYPE)
                        .build())
                .trackable(false)
                .id(UUID.randomUUID().toString())
                .properties(properties)
                .build();
    }
}
