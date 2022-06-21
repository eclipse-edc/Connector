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

import com.github.javafaker.Faker;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.common.token.TokenValidationService;
import org.eclipse.dataspaceconnector.dataplane.spi.DataPlaneConstants;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.OutputStreamDataSinkFactory;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
class DataPlanePublicApiControllerTest {

    private static final Faker FAKER = new Faker();

    private final DataPlaneManager dataPlaneManager = mock(DataPlaneManager.class);
    private final TokenValidationService tokenValidationService = mock(TokenValidationService.class);
    private final int port = getFreePort();
    private final int validationPort = getFreePort();

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.registerSystemExtension(ServiceExtension.class, new TestServiceExtension());
        extension.setConfiguration(Map.of(
                "web.http.public.port", String.valueOf(port),
                "web.http.public.path", "/public",
                "web.http.control.port", String.valueOf(getFreePort()),
                "web.http.validation.port", String.valueOf(validationPort),
                "web.http.validation.path", "/validation",
                "edc.controlplane.validation-endpoint", "http://localhost:" + validationPort + "/validation/token"
        ));
    }

    @Test
    void postFailure_missingTokenInRequest() {
        given()
                .port(port)
                .when()
                .post("/public/any")
                .then()
                .statusCode(401)
                .body("errors[0]", is("Missing bearer token"));
    }

    @Test
    void postFailure_tokenValidationFailure() {
        var token = FAKER.internet().uuid();
        var errorMsg = FAKER.internet().uuid();
        when(tokenValidationService.validate(token)).thenReturn(Result.failure(errorMsg));

        given()
                .port(port)
                .header(AUTHORIZATION, token)
                .when()
                .post("/public/any")
                .then()
                .statusCode(401)
                .body("errors.size()", is(1));
        verify(tokenValidationService).validate(token);
    }

    @Test
    void postFailure_requestValidationFailure() {
        var token = FAKER.internet().uuid();
        var errorMsg = FAKER.internet().uuid();
        var claimsToken = createClaimsToken(testDestAddress());
        when(tokenValidationService.validate(token)).thenReturn(Result.success(claimsToken));
        when(dataPlaneManager.validate(any())).thenReturn(Result.failure(errorMsg));

        given()
                .port(port)
                .header(AUTHORIZATION, token)
                .when()
                .post("/public/any")
                .then()
                .statusCode(400)
                .body("errors.size()", is(1));
    }

    @Test
    void postFailure_transferFailure() {
        var token = FAKER.internet().uuid();
        var errorMsg = FAKER.internet().uuid();
        var claims = createClaimsToken(testDestAddress());
        when(tokenValidationService.validate(token)).thenReturn(Result.success(claims));
        when(dataPlaneManager.validate(any())).thenReturn(Result.success(true));
        when(dataPlaneManager.transfer(any(DataSink.class), any()))
                .thenReturn(completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, errorMsg)));

        given()
                .port(port)
                .header(AUTHORIZATION, token)
                .when()
                .post("/public/any")
                .then()
                .statusCode(500)
                .body("errors[0]", is(errorMsg));
    }

    @Test
    void postFailure_transferErrorUnhandledException() {
        var token = FAKER.internet().uuid();
        var errorMsg = FAKER.internet().uuid();
        var claims = createClaimsToken(testDestAddress());
        when(tokenValidationService.validate(token)).thenReturn(Result.success(claims));
        when(dataPlaneManager.validate(any())).thenReturn(Result.success(true));
        when(dataPlaneManager.transfer(any(DataSink.class), any(DataFlowRequest.class)))
                .thenReturn(failedFuture(new RuntimeException(errorMsg)));

        given()
                .port(port)
                .header(AUTHORIZATION, token)
                .when()
                .post("/public/any")
                .then()
                .statusCode(500)
                .body("errors[0]", is("Unhandled exception: " + errorMsg));
    }

    @Test
    void postSuccess() {
        var token = FAKER.internet().uuid();
        var address = testDestAddress();
        var claimsToken = createClaimsToken(address);
        var requestCaptor = ArgumentCaptor.forClass(DataFlowRequest.class);

        when(tokenValidationService.validate(anyString())).thenReturn(Result.success(claimsToken));
        when(dataPlaneManager.validate(any())).thenReturn(Result.success(true));
        when(dataPlaneManager.transfer(any(DataSink.class), any()))
                .thenReturn(completedFuture(StatusResult.success()));

        given()
                .port(port)
                .header(AUTHORIZATION, token)
                .when()
                .post("/public/any?foo=bar")
                .then()
                .statusCode(200);

        verify(dataPlaneManager, times(1)).validate(requestCaptor.capture());
        verify(dataPlaneManager, times(1)).transfer(ArgumentCaptor.forClass(DataSink.class).capture(), requestCaptor.capture());
        var capturedRequests = requestCaptor.getAllValues();
        assertThat(capturedRequests)
                .hasSize(2)
                .allSatisfy(request -> {
                    assertThat(request.getDestinationDataAddress().getType()).isEqualTo(OutputStreamDataSinkFactory.TYPE);
                    assertThat(request.getSourceDataAddress().getType()).isEqualTo(address.getType());
                    assertThat(request.getProperties()).containsEntry("method", "POST").containsEntry("pathSegments", "any").containsEntry("queryParams", "foo=bar");
                });
    }

    private ClaimToken createClaimsToken(DataAddress address) {
        return ClaimToken.Builder.newInstance().claim(DataPlaneConstants.DATA_ADDRESS, new TypeManager().writeValueAsString(address)).build();
    }

    private DataAddress testDestAddress() {
        return DataAddress.Builder.newInstance().type("test").build();
    }

    @Provides(DataPlaneManager.class)
    private class TestServiceExtension implements ServiceExtension {

        @Inject
        WebService webService;

        @Override
        public void initialize(ServiceExtensionContext context) {
            context.registerService(DataPlaneManager.class, dataPlaneManager);
            webService.registerResource("validation", new TestValidationController());
        }
    }

    @Path("/")
    public class TestValidationController {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("/token")
        public ClaimToken validate(@HeaderParam("Authorization") String token) {
            var result = tokenValidationService.validate(token);
            if (result.succeeded()) {
                return result.getContent();
            } else {
                throw new IllegalArgumentException("Token is not valid: " + String.join(", ", result.getFailureMessages()));
            }

        }
    }

}
