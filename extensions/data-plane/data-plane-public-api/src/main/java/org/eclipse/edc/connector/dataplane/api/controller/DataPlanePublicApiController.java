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
 *       Mercedes-Benz Tech Innovation GmbH - publish public api context into dedicated swagger hub page
 *
 */

package org.eclipse.edc.connector.dataplane.api.controller;

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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.edc.connector.dataplane.api.pipeline.ProxyStreamPayload;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.resolver.DataAddressResolver;
import org.eclipse.edc.connector.dataplane.spi.response.TransferErrorResponse;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.WILDCARD;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.status;
import static java.lang.String.format;
import static java.lang.String.join;

@Path("{any:.*}")
@Produces(WILDCARD)
public class DataPlanePublicApiController implements DataPlanePublicApi {

    private final PipelineService pipelineService;
    private final DataAddressResolver dataAddressResolver;
    private final DataFlowRequestSupplier requestSupplier;

    public DataPlanePublicApiController(PipelineService pipelineService,
                                        DataAddressResolver dataAddressResolver) {
        this.pipelineService = pipelineService;
        this.dataAddressResolver = dataAddressResolver;
        this.requestSupplier = new DataFlowRequestSupplier();
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
            response.resume(error(BAD_REQUEST, "Missing token"));
            return;
        }

        var tokenValidation = dataAddressResolver.resolve(token);
        if (tokenValidation.failed()) {
            response.resume(error(FORBIDDEN, tokenValidation.getFailureDetail()));
            return;
        }

        var dataAddress = tokenValidation.getContent();
        var dataFlowRequest = requestSupplier.apply(contextApi, dataAddress);

        var validationResult = pipelineService.validate(dataFlowRequest);
        if (validationResult.failed()) {
            var errorMsg = validationResult.getFailureMessages().isEmpty() ?
                    format("Failed to validate request with id: %s", dataFlowRequest.getId()) :
                    join(",", validationResult.getFailureMessages());
            response.resume(error(BAD_REQUEST, errorMsg));
            return;
        }

        pipelineService.transfer(dataFlowRequest)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        var responseObject = result
                                .map(content -> {
                                    if (result.getContent() instanceof ProxyStreamPayload payload) {
                                        return Response.ok(streamToOutput(payload.inputStream())).type(payload.contentType()).build();
                                    } else {
                                        return Response.ok(result.getContent()).build();
                                    }
                                })
                                .orElse(failure -> error(INTERNAL_SERVER_ERROR, failure.getFailureDetail()));

                        response.resume(responseObject);
                    } else {
                        var error = "Unhandled exception occurred during data transfer: " + throwable.getMessage();
                        response.resume(error(INTERNAL_SERVER_ERROR, error));
                    }
                });
    }

    @NotNull
    private static StreamingOutput streamToOutput(InputStream input) {
        return output -> {
            try (input) {
                input.transferTo(output);
            }
        };
    }

    private static Response error(Response.Status status, String error) {
        return status(status).type(APPLICATION_JSON).entity(new TransferErrorResponse(List.of(error))).build();
    }

}
