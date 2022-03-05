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

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.dataspaceconnector.common.token.TokenValidationService;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSinkFactory.TYPE;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.BODY;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.MEDIA_TYPE;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.METHOD;
import static org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema.QUERY_PARAMS;
import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.DATA_ADDRESS_CLAIM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlanePublicApiRequestFilterTest {

    private TokenValidationService tokenValidationService;
    private DataPlaneManager dataPlaneManager;
    private DataPlanePublicApiRequestFilter filter;
    private TypeManager typeManager;

    @BeforeEach
    public void setUp() {
        tokenValidationService = mock(TokenValidationService.class);
        dataPlaneManager = mock(DataPlaneManager.class);
        typeManager = new TypeManager();
        var monitor = mock(Monitor.class);
        filter = new DataPlanePublicApiRequestFilter(tokenValidationService, dataPlaneManager, monitor, typeManager);
    }

    /**
     * OK test: check that {@link DataFlowRequest} is properly created and that success response is returned.
     */
    @Test
    void verifyDataFlowRequest() throws IOException {
        var claims = createClaimToken();
        var body = testJsonBody();
        var context = createDefaultContext(testQueryParams(), MediaType.valueOf(APPLICATION_JSON), body);
        var data = "bar".getBytes();
        var dataSource = new InputStreamDataSource("foo", new ByteArrayInputStream(data));

        when(tokenValidationService.validate(anyString())).thenReturn(Result.success(claims));

        var validationCapture = ArgumentCaptor.forClass(DataFlowRequest.class);
        var transferCapture = ArgumentCaptor.forClass(DataFlowRequest.class);

        when(dataPlaneManager.validate(validationCapture.capture())).thenReturn(Result.success(true));
        when(dataPlaneManager.transfer(any(OutputStreamDataSink.class), transferCapture.capture()))
                .then(invocation -> ((OutputStreamDataSink) invocation.getArguments()[0]).transfer(dataSource))
                .thenReturn(CompletableFuture.completedFuture(TransferResult.success()));

        filter.filter(context);

        var responseCapture = ArgumentCaptor.forClass(Response.class);
        verify(context, times(1)).abortWith(responseCapture.capture());
        assertSuccessResponse(responseCapture.getValue(), "bar");
        assertThat(validationCapture.getValue())
                .isNotNull()
                .isEqualTo(transferCapture.getValue())
                .satisfies(dataFlowRequest -> {
                    assertThat(dataFlowRequest.isTrackable()).isFalse();
                    assertThat(dataFlowRequest.getSourceDataAddress().getType()).isEqualTo("test");
                    assertThat(dataFlowRequest.getDestinationDataAddress().getType()).isEqualTo(TYPE);
                    assertThat(dataFlowRequest.getProperties())
                            .containsEntry(METHOD, HttpMethod.POST)
                            .containsEntry(QUERY_PARAMS, "foo=bar&hello=world")
                            .containsEntry(MEDIA_TYPE, APPLICATION_JSON)
                            .containsEntry(BODY, body);
                });
    }

    /**
     * Check that response with code 401 (not authorized) is returned in case no token is provided.
     */
    @Test
    void verifyDataFlowRequest_missingTokenInRequest() throws IOException {
        var context = mock(ContainerRequestContext.class);
        when(context.getHeaderString("Authorization")).thenReturn(null);

        filter.filter(context);

        var responseCapture = ArgumentCaptor.forClass(Response.class);
        verify(context, times(1)).abortWith(responseCapture.capture());
        assertErrorResponse(responseCapture.getValue(), 401, "Missing bearer token");
    }

    /**
     * Check that response with code 401 (not authorized) is returned in case token validation fails.
     */
    @Test
    void verifyDataFlowRequest_tokenValidationFailure() throws IOException {
        var context = createDefaultContext();
        when(tokenValidationService.validate(anyString())).thenReturn(Result.failure("token validation error"));

        filter.filter(context);

        var responseCapture = ArgumentCaptor.forClass(Response.class);
        verify(context, times(1)).abortWith(responseCapture.capture());
        assertErrorResponse(responseCapture.getValue(), 401, "token validation error");
    }

    /**
     * Check that response with code 400 (bad request) is returned in case of request validation error.
     */
    @Test
    void verifyDataFlowRequest_requestValidationFailure() throws IOException {
        var claims = createClaimToken();
        var context = createDefaultContext();

        when(tokenValidationService.validate(anyString())).thenReturn(Result.success(claims));

        when(dataPlaneManager.validate(any(DataFlowRequest.class))).thenReturn(Result.failure("request validation error"));

        filter.filter(context);

        var responseCapture = ArgumentCaptor.forClass(Response.class);
        verify(context, times(1)).abortWith(responseCapture.capture());
        assertErrorResponse(responseCapture.getValue(), 400, "request validation error");
    }

    /**
     * Check that response with code 500 (internal server error) is returned in case of error during data transfer.
     */
    @Test
    void verifyDataFlowRequest_transferFailure() throws IOException {
        var claims = createClaimToken();
        var context = createDefaultContext();

        when(tokenValidationService.validate(anyString())).thenReturn(Result.success(claims));

        when(dataPlaneManager.validate(any(DataFlowRequest.class))).thenReturn(Result.success(true));
        when(dataPlaneManager.transfer(any(DataSink.class), any(DataFlowRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(TransferResult.failure(ResponseStatus.FATAL_ERROR, "transfer error")));

        filter.filter(context);

        var responseCapture = ArgumentCaptor.forClass(Response.class);
        verify(context, times(1)).abortWith(responseCapture.capture());
        assertErrorResponse(responseCapture.getValue(), 500, "transfer error");
    }

    /**
     * Check that response with code 500 (internal server error) is returned in case an unhandled exception is raised during data transfer.
     */
    @Test
    void verifyDataFlowRequest_transferErrorUnhandledException() throws IOException {
        var claims = createClaimToken();
        var context = createDefaultContext();

        when(tokenValidationService.validate(anyString())).thenReturn(Result.success(claims));

        when(dataPlaneManager.validate(any(DataFlowRequest.class))).thenReturn(Result.success(true));
        when(dataPlaneManager.transfer(any(DataSink.class), any(DataFlowRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("transfer exception")));

        filter.filter(context);

        var responseCapture = ArgumentCaptor.forClass(Response.class);
        verify(context, times(1)).abortWith(responseCapture.capture());
        assertErrorResponse(responseCapture.getValue(), 500, "Unhandled exception: transfer exception");
    }

    private static void assertSuccessResponse(Response response, String message) {
        assertResponse(response, 200, "data", message);
    }

    private static void assertErrorResponse(Response response, int errorCode, String message) {
        assertResponse(response, errorCode, "errors", List.of(message));
    }

    private static void assertResponse(Response response, int expectedStatusCode, String expectedEntityKey, Object expectedEntityValue) {
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(expectedStatusCode);
        assertThat(response.getEntity())
                .isInstanceOf(Map.class)
                .satisfies(o -> assertThat((Map) o)
                        .hasSize(1)
                        .containsEntry(expectedEntityKey, expectedEntityValue));
    }

    private MultivaluedMap<String, String> testQueryParams() {
        var queryParams = new MultivaluedHashMap<String, String>();
        queryParams.put("foo", Arrays.asList("bar", "hey"));
        queryParams.put("hello", List.of("world"));
        return queryParams;
    }

    private String testJsonBody() {
        return "{ \"foo\" : \"bar\"}";
    }

    private ContainerRequestContext createDefaultContext() {
        return createDefaultContext(testQueryParams(), null, null);
    }

    private ContainerRequestContext createDefaultContext(MultivaluedMap<String, String> queryParams, @Nullable MediaType mediaType, @Nullable String body) {
        var uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters()).thenReturn(queryParams);

        var context = mock(ContainerRequestContext.class);
        when(context.getUriInfo()).thenReturn(uriInfo);
        when(context.getMethod()).thenReturn(HttpMethod.POST);
        when(context.getHeaderString("Authorization")).thenReturn("test-token");

        if (mediaType != null && body != null) {
            when(context.hasEntity()).thenReturn(true);
            when(context.getMediaType()).thenReturn(mediaType);
            when(context.getEntityStream()).thenReturn(new ByteArrayInputStream(body.getBytes()));
        }

        return context;
    }

    private ClaimToken createClaimToken() {
        var dataAddress = DataAddress.Builder.newInstance().type("test").build();
        return ClaimToken.Builder.newInstance()
                .claim(DATA_ADDRESS_CLAIM, typeManager.writeValueAsString(dataAddress))
                .build();
    }
}
