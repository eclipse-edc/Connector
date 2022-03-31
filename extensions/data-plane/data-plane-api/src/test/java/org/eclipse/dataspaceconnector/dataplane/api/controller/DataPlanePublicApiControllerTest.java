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

import com.github.javafaker.Faker;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.common.token.TokenValidationService;
import org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSinkFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.response.TransferErrorResponse;
import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlanePublicApiControllerTest {

    private static final Faker FAKER = new Faker();

    private static final TypeManager TYPE_MANAGER = new TypeManager();

    private DataPlaneManager dataPlaneManagerMock;
    private TokenValidationService tokenValidationServiceMock;
    private ContainerRequestContextApi requestContextWrapperMock;
    private DataPlanePublicApiController controller;

    @BeforeEach
    void setUp() {
        dataPlaneManagerMock = mock(DataPlaneManager.class);
        tokenValidationServiceMock = mock(TokenValidationService.class);
        requestContextWrapperMock = mock(ContainerRequestContextApi.class);
        controller = new DataPlanePublicApiController(dataPlaneManagerMock, tokenValidationServiceMock, mock(Monitor.class), requestContextWrapperMock, TYPE_MANAGER, Executors.newSingleThreadExecutor());
    }

    /**
     * Check that response with code 401 (not authorized) is returned in case no token is provided.
     */
    @Test
    void postFailure_missingTokenInRequest() {
        when(requestContextWrapperMock.authHeader(any())).thenReturn(null);

        var response = controller.post(null);

        assertErrorResponse(response, 401, "Missing bearer token");
    }

    /**
     * Check that response with code 401 (not authorized) is returned in case token validation fails.
     */
    @Test
    void postFailure_tokenValidationFailure() {
        var token = FAKER.internet().uuid();
        var errorMsg = FAKER.internet().uuid();
        when(requestContextWrapperMock.authHeader(any())).thenReturn(token);
        when(tokenValidationServiceMock.validate(token)).thenReturn(Result.failure(errorMsg));

        var response = controller.post(null);

        assertErrorResponse(response, 401, errorMsg);
    }

    /**
     * Check that response with code 400 (bad request) is returned in case of request validation error.
     */
    @Test
    void postFailure_requestValidationFailure() {
        var token = FAKER.internet().uuid();
        var errorMsg = FAKER.internet().uuid();
        var claims = createClaimsToken(testDestAddress());
        when(requestContextWrapperMock.authHeader(any())).thenReturn(token);
        when(tokenValidationServiceMock.validate(token)).thenReturn(Result.success(claims));
        when(requestContextWrapperMock.properties(any())).thenReturn(Map.of());
        when(dataPlaneManagerMock.validate(any())).thenReturn(Result.failure(errorMsg));

        var response = controller.post(null);

        verify(dataPlaneManagerMock, times(1)).validate(any());

        assertErrorResponse(response, 400, errorMsg);
    }

    /**
     * Check that response with code 500 (internal server error) is returned in case of error during data transfer.
     */
    @Test
    void postFailure_transferFailure() {
        var token = FAKER.internet().uuid();
        var errorMsg = FAKER.internet().uuid();
        var claims = createClaimsToken(testDestAddress());
        when(requestContextWrapperMock.authHeader(any())).thenReturn(token);
        when(tokenValidationServiceMock.validate(token)).thenReturn(Result.success(claims));
        when(requestContextWrapperMock.properties(any())).thenReturn(Map.of());
        when(dataPlaneManagerMock.validate(any())).thenReturn(Result.success(true));
        when(dataPlaneManagerMock.transfer(any(DataSink.class), any()))
                .thenReturn(CompletableFuture.completedFuture(TransferResult.failure(ResponseStatus.FATAL_ERROR, errorMsg)));

        var response = controller.post(null);

        assertErrorResponse(response, 500, errorMsg);
    }

    /**
     * Check that response with code 500 (internal server error) is returned in case an unhandled exception is raised during data transfer.
     */
    @Test
    void postFailure_transferErrorUnhandledException() {
        var token = FAKER.internet().uuid();
        var errorMsg = FAKER.internet().uuid();
        var claims = createClaimsToken(testDestAddress());
        when(requestContextWrapperMock.authHeader(any())).thenReturn(token);
        when(tokenValidationServiceMock.validate(token)).thenReturn(Result.success(claims));
        when(requestContextWrapperMock.properties(any())).thenReturn(Map.of());
        when(dataPlaneManagerMock.validate(any())).thenReturn(Result.success(true));
        when(dataPlaneManagerMock.transfer(any(DataSink.class), any(DataFlowRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException(errorMsg)));

        var response = controller.post(null);

        assertErrorResponse(response, 500, "Unhandled exception: " + errorMsg);
    }

    @Test
    void postSuccess() {
        var token = FAKER.internet().uuid();
        var address = testDestAddress();
        var claimsToken = createClaimsToken(address);
        var requestCaptor = ArgumentCaptor.forClass(DataFlowRequest.class);
        var requestProperties = testRequestProperties();

        when(requestContextWrapperMock.authHeader(any())).thenReturn(token);
        when(tokenValidationServiceMock.validate(anyString())).thenReturn(Result.success(claimsToken));
        when(requestContextWrapperMock.properties(any())).thenReturn(requestProperties);
        when(dataPlaneManagerMock.validate(any())).thenReturn(Result.success(true));
        when(dataPlaneManagerMock.transfer(any(DataSink.class), any()))
                .thenReturn(CompletableFuture.completedFuture(TransferResult.success()));

        var response = controller.post(null);

        verify(dataPlaneManagerMock, times(1)).validate(requestCaptor.capture());
        verify(dataPlaneManagerMock, times(1)).transfer(ArgumentCaptor.forClass(DataSink.class).capture(), requestCaptor.capture());

        assertThat(response.getStatus()).isEqualTo(200);
        var capturedRequests = requestCaptor.getAllValues();

        assertThat(capturedRequests)
                .hasSize(2)
                .allSatisfy(request -> {
                    assertThat(request.getDestinationDataAddress().getType()).isEqualTo(OutputStreamDataSinkFactory.TYPE);
                    assertThat(request.getSourceDataAddress().getType()).isEqualTo(address.getType());
                    assertThat(request.getProperties()).containsExactlyInAnyOrderEntriesOf(requestProperties);
                });
    }

    private static ClaimToken createClaimsToken(DataAddress address) {
        return ClaimToken.Builder.newInstance().claim(DataPlaneConstants.DATA_ADDRESS, TYPE_MANAGER.writeValueAsString(address)).build();
    }

    private static DataAddress testDestAddress() {
        return DataAddress.Builder.newInstance().type("test").build();
    }

    private static Map<String, String> testRequestProperties() {
        return Map.of("foo", "bar");
    }

    /**
     * Assert an error has been returned and check the message.
     */
    private static void assertErrorResponse(Response response, int errorCode, String message) {
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(errorCode);

        var entity = response.getEntity();
        assertThat(entity).isInstanceOf(TransferErrorResponse.class);
        var errorResponse = (TransferErrorResponse) entity;
        assertThat(errorResponse.getErrors()).containsExactly(message);
    }
}