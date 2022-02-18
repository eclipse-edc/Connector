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
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.dataplane.spi.schema.DataFlowRequestSchema;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
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
        var context = createContext(testQueryParams());
        var data = "bar".getBytes();
        var dataSource = new InputStreamDataSource("foo", new ByteArrayInputStream(data));

        when(tokenValidationService.validate(anyString())).thenReturn(Result.success(claims));

        var validationCapture = ArgumentCaptor.forClass(DataFlowRequest.class);
        var transferCapture = ArgumentCaptor.forClass(DataFlowRequest.class);
        var responseCapture = ArgumentCaptor.forClass(Response.class);

        when(dataPlaneManager.validate(validationCapture.capture())).thenReturn(Result.success(true));
        when(dataPlaneManager.transfer(any(OutputStreamDataSink.class), transferCapture.capture()))
                .then(invocation -> ((OutputStreamDataSink) invocation.getArguments()[0]).transfer(dataSource))
                .thenReturn(CompletableFuture.completedFuture(TransferResult.success()));
        doNothing().when(context).abortWith(responseCapture.capture());

        filter.filter(context);

        verify(context, times(1)).abortWith(any(Response.class));

        assertThat(responseCapture.getValue())
                .isNotNull()
                .satisfies(response -> {
                    assertThat(response.getStatus()).isEqualTo(200);
                    assertThat(response.getEntity())
                            .isInstanceOf(Map.class)
                            .satisfies(o -> assertThat((Map) o).containsEntry("data", "bar"));
                });
        assertThat(validationCapture.getValue())
                .isNotNull()
                .isEqualTo(transferCapture.getValue())
                .satisfies(dataFlowRequest -> {
                    assertThat(dataFlowRequest.isTrackable()).isFalse();
                    assertThat(dataFlowRequest.getSourceDataAddress().getType()).isEqualTo("test");
                    assertThat(dataFlowRequest.getProperties())
                            .containsEntry(DataFlowRequestSchema.METHOD, "GET")
                            .containsEntry(DataFlowRequestSchema.QUERY_PARAMS, "foo=bar&hello=world");
                });
    }

    /**
     * Check that response with code 401 (not authorized) is returned in case of token validation error.
     */
    @Test
    void verifyDataFlowRequest_tokenValidationFailure() throws IOException {
        var context = createContext(testQueryParams());
        when(tokenValidationService.validate(anyString())).thenReturn(Result.failure("token validation error"));
        var responseCapture = ArgumentCaptor.forClass(Response.class);

        doNothing().when(context).abortWith(responseCapture.capture());

        filter.filter(context);

        verify(context, times(1)).abortWith(any(Response.class));

        assertThat(responseCapture.getValue())
                .isNotNull()
                .satisfies(response -> {
                    assertThat(response.getStatus()).isEqualTo(401);
                    assertThat(response.getEntity())
                            .isInstanceOf(Map.class)
                            .satisfies(o -> assertThat((Map) o).containsEntry("errors", Arrays.asList("token validation error")));
                });
    }

    /**
     * Check that response with code 400 (bad request) is returned in case of request validation error.
     */
    @Test
    void verifyDataFlowRequest_requestValidationFailure() throws IOException {
        var claims = createClaimToken();
        var context = createContext(testQueryParams());

        when(tokenValidationService.validate(anyString())).thenReturn(Result.success(claims));

        var responseCapture = ArgumentCaptor.forClass(Response.class);

        when(dataPlaneManager.validate(any(DataFlowRequest.class))).thenReturn(Result.failure("request validation error"));

        doNothing().when(context).abortWith(responseCapture.capture());

        filter.filter(context);

        verify(context, times(1)).abortWith(any(Response.class));

        assertThat(responseCapture.getValue())
                .isNotNull()
                .satisfies(response -> {
                    assertThat(response.getStatus()).isEqualTo(400);
                    assertThat(response.getEntity())
                            .isInstanceOf(Map.class)
                            .satisfies(o -> assertThat((Map) o).containsEntry("errors", Arrays.asList("request validation error")));
                });
    }

    /**
     * Check that response with code 500 (internal server error) is returned in case of error during data transfer.
     */
    @Test
    void verifyDataFlowRequest_transferFailure() throws IOException {
        var claims = createClaimToken();
        var context = createContext(testQueryParams());

        when(tokenValidationService.validate(anyString())).thenReturn(Result.success(claims));

        var responseCapture = ArgumentCaptor.forClass(Response.class);

        when(dataPlaneManager.validate(any(DataFlowRequest.class))).thenReturn(Result.success(true));
        when(dataPlaneManager.transfer(any(DataSink.class), any(DataFlowRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(TransferResult.failure(ResponseStatus.FATAL_ERROR, "transfer error")));
        doNothing().when(context).abortWith(responseCapture.capture());

        filter.filter(context);

        verify(context, times(1)).abortWith(any(Response.class));

        assertThat(responseCapture.getValue())
                .isNotNull()
                .satisfies(response -> {
                    assertThat(response.getStatus()).isEqualTo(500);
                    assertThat(response.getEntity())
                            .isInstanceOf(Map.class)
                            .satisfies(o -> assertThat((Map) o).containsEntry("errors", Arrays.asList("transfer error")));
                });
    }

    /**
     * Check that response with code 500 (internal server error) is returned in case an unhandled exception is raised during data transfer.
     */
    @Test
    void verifyDataFlowRequest_transferErrorUnhandledException() throws IOException {
        var claims = createClaimToken();
        var context = createContext(testQueryParams());

        when(tokenValidationService.validate(anyString())).thenReturn(Result.success(claims));

        var responseCapture = ArgumentCaptor.forClass(Response.class);

        when(dataPlaneManager.validate(any(DataFlowRequest.class))).thenReturn(Result.success(true));
        when(dataPlaneManager.transfer(any(DataSink.class), any(DataFlowRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("transfer exception")));
        doNothing().when(context).abortWith(responseCapture.capture());

        filter.filter(context);

        verify(context, times(1)).abortWith(any(Response.class));

        assertThat(responseCapture.getValue())
                .isNotNull()
                .satisfies(response -> {
                    assertThat(response.getStatus()).isEqualTo(500);
                    assertThat(response.getEntity())
                            .isInstanceOf(Map.class)
                            .satisfies(o -> assertThat((Map) o).containsEntry("errors", Arrays.asList("Unhandled exception: transfer exception")));
                });
    }

    private MultivaluedMap<String, String> testQueryParams() {
        var queryParams = new MultivaluedHashMap<String, String>();
        queryParams.put("foo", Arrays.asList("bar", "hey"));
        queryParams.put("hello", Arrays.asList("world"));
        return queryParams;
    }

    private ContainerRequestContext createContext(MultivaluedMap<String, String> queryParams) {
        var uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters()).thenReturn(queryParams);

        var request = mock(Request.class);
        when(request.getMethod()).thenReturn("GET");

        var context = mock(ContainerRequestContext.class);
        when(context.getUriInfo()).thenReturn(uriInfo);
        when(context.getRequest()).thenReturn(request);
        when(context.getHeaderString("Authorization")).thenReturn("test-token");
        return context;
    }

    private ClaimToken createClaimToken() {
        var dataAddress = DataAddress.Builder.newInstance().type("test").build();
        return ClaimToken.Builder.newInstance()
                .claim(EndpointDataReferenceClaimsSchema.DATA_ADDRESS_CLAIM, typeManager.writeValueAsString(dataAddress))
                .build();
    }
}