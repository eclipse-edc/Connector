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

package org.eclipse.dataspaceconnector.dataplane.api.transfer;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.dataspaceconnector.common.token.TokenValidationService;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSinkFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
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
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.BODY;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.MEDIA_TYPE;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.METHOD;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.QUERY_PARAMS;
import static org.eclipse.dataspaceconnector.spi.types.domain.dataplane.DataPlaneConstants.DATA_ADDRESS;

/**
 * Filter that intercepts call to public API of the data plane. Note that a request filter is preferred over a Controller here
 * as public API of the data plane is supposed to support any verb (GET,PUT,POST...), while this verb is just forwarded to the data source.
 * Thus, this approach allows to have one single implementation that will process all requests, regardless of the verb, instead of having one endpoint dedicated to each verb.
 */
@PreMatching
public class DataPlanePublicApiRequestFilter implements ContainerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final TokenValidationService tokenValidationService;
    private final DataPlaneManager dataPlaneManager;
    private final Monitor monitor;
    private final TypeManager typeManager;

    public DataPlanePublicApiRequestFilter(TokenValidationService tokenValidationService, DataPlaneManager dataPlaneManager, Monitor monitor, TypeManager typeManager) {
        this.tokenValidationService = tokenValidationService;
        this.dataPlaneManager = dataPlaneManager;
        this.monitor = monitor;
        this.typeManager = typeManager;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var bearerToken = requestContext.getHeaderString(AUTHORIZATION_HEADER);
        if (bearerToken == null) {
            requestContext.abortWith(notAuthorizedErrors(List.of("Missing bearer token")));
            return;
        }

        // validate and decode input token
        var tokenValidationResult = tokenValidationService.validate(bearerToken);
        if (tokenValidationResult.failed()) {
            requestContext.abortWith(notAuthorizedErrors(tokenValidationResult.getFailureMessages()));
            return;
        }

        // create request and perform validation
        var dataFlowRequest = createDataFlowRequest(tokenValidationResult.getContent(), requestContext);
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

    /**
     * Create a {@link DataFlowRequest} based on the decoded claim token and the request content.
     */
    private DataFlowRequest createDataFlowRequest(ClaimToken claims, ContainerRequestContext requestContext) throws IOException {
        var dataAddress = typeManager.readValue(claims.getClaims().get(DATA_ADDRESS), DataAddress.class);
        var requestProperties = createDataFlowRequestProperties(requestContext);
        return DataFlowRequest.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(dataAddress)
                .destinationDataAddress(DataAddress.Builder.newInstance()
                        .type(OutputStreamDataSinkFactory.TYPE)
                        .build())
                .trackable(false)
                .id(UUID.randomUUID().toString())
                .properties(requestProperties)
                .build();
    }

    /**
     * Map relevant information from the container request context to the properties of the {@link DataFlowRequest}.
     */
    private static Map<String, String> createDataFlowRequestProperties(ContainerRequestContext requestContext) throws IOException {
        var requestProperties = new HashMap<String, String>();
        requestProperties.put(METHOD, requestContext.getMethod());
        requestProperties.put(QUERY_PARAMS, convertQueryParamsToString(requestContext.getUriInfo()));
        if (requestContext.hasEntity()) {
            requestProperties.put(MEDIA_TYPE, requestContext.getMediaType().toString());
            try (InputStream in = requestContext.getEntityStream()) {
                requestProperties.put(BODY, new String(in.readAllBytes()));
            }
        }
        return requestProperties;
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
