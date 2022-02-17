/*
 *  Copyright (c) 2020, 2021 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.dataspaceconnector.dataplane.api.transfer;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.eclipse.dataspaceconnector.dataplane.api.common.ResponseFunctions.internalErrors;
import static org.eclipse.dataspaceconnector.dataplane.api.common.ResponseFunctions.notAuthorizedErrors;
import static org.eclipse.dataspaceconnector.dataplane.api.common.ResponseFunctions.validationError;
import static org.eclipse.dataspaceconnector.dataplane.api.common.ResponseFunctions.validationErrors;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.METHOD;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.QUERY_PARAMS;

/**
 * Filter that intercepts call to public API of the data plane.
 */
public class DataPlanePublicApiRequestFilter implements ContainerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final TokenValidationService tokenValidationService;
    private final DataPlaneManager dataPlaneManager;
    private final Monitor monitor;
    private final TypeManager typeManager;

    public DataPlanePublicApiRequestFilter(@NotNull TokenValidationService tokenValidationService,
                                           @NotNull DataPlaneManager dataPlaneManager,
                                           @NotNull Monitor monitor,
                                           @NotNull TypeManager typeManager) {
        this.tokenValidationService = tokenValidationService;
        this.dataPlaneManager = dataPlaneManager;
        this.monitor = monitor;
        this.typeManager = typeManager;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var bearerToken = requestContext.getHeaderString(AUTHORIZATION_HEADER);

        // validate and decode input token
        var tokenValidationResult = tokenValidationService.validate(bearerToken);
        if (tokenValidationResult.failed()) {
            requestContext.abortWith(notAuthorizedErrors(tokenValidationResult.getFailureMessages()));
            return;
        }

        // create request and perform validation
        var dataFlowRequest = createDataFlowRequest(tokenValidationResult.getContent(), requestContext.getUriInfo(), requestContext.getRequest());
        var validationResult = dataPlaneManager.validate(dataFlowRequest);
        if (validationResult.failed()) {
            requestContext.abortWith(validationResult.getFailureMessages().isEmpty() ?
                    validationError(String.format("Failed to validate request with id: %s", dataFlowRequest.getId())) :
                    validationErrors(validationResult.getFailureMessages()));
            return;
        }

        // perform the data transfer
        var stream = new ByteArrayOutputStream();
        var sink = new OutputStreamDataSink(stream, executorService, monitor);
        var transferResult = dataPlaneManager.transfer(sink, dataFlowRequest)
                .exceptionally(throwable -> TransferResult.failure(ResponseStatus.FATAL_ERROR, "Unhandled exception: " + throwable.getMessage()))
                .join();
        if (transferResult.failed()) {
            requestContext.abortWith(internalErrors(transferResult.getFailureMessages()));
            return;
        }

        requestContext.abortWith(Response.ok().entity(Map.of("data", stream.toString())).build());
    }

    private DataFlowRequest createDataFlowRequest(ClaimToken claims, UriInfo queryParams, Request method) {
        var dataAddress = typeManager.readValue(claims.getClaims().get(EndpointDataReferenceClaimsSchema.DATA_ADDRESS_CLAIM), DataAddress.class);
        var queryParamsAsString = convertQueryParamsToString(queryParams);
        return DataFlowRequest.Builder.newInstance()
                .processId(UUID.randomUUID().toString()) // TODO: map the transfer process id into the token?
                .sourceDataAddress(dataAddress)
                .destinationDataAddress(DataAddress.Builder.newInstance().type("dummy").build())
                .trackable(false)
                .id(UUID.randomUUID().toString())
                .properties(Map.of(METHOD, method.getMethod(), QUERY_PARAMS, queryParamsAsString)) // TODO: map body
                .build();
    }

    /**
     * Convert query parameters injected from the input into a string, e.g. foo=bar&hello=world
     */
    private static String convertQueryParamsToString(UriInfo uriInfo) {
        return uriInfo.getQueryParameters().entrySet()
                .stream()
                .map(entry -> new QueryParam(entry.getKey(), entry.getValue()))
                .filter(QueryParam::isValid)
                .map(QueryParam::toString)
                .collect(Collectors.joining("&"));
    }

    private static final class QueryParam {

        private final String key;
        private final List<String> values;
        private final boolean valid;

        private QueryParam(String key, List<String> values) {
            this.key = key;
            this.values = values;
            this.valid = key != null && values != null && !values.isEmpty();
        }

        public boolean isValid() {
            return valid;
        }

        @Override
        public String toString() {
            return valid ? key + "=" + values.get(0) : "";
        }
    }
}
