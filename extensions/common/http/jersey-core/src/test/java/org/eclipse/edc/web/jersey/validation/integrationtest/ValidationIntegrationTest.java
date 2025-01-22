/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.web.jersey.validation.integrationtest;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.web.spi.validation.InterceptorFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This test aims at showing how custom validation can be done end-to-end:
 * - inject the validation registry
 * - register functions (different variants)
 * - invoke the controller method through a REST call
 * - verify that the function(s) got invoked, verify that the correct HTTP error results
 * <p>
 * Please note that the focus of this test is on demonstration rather than full coverage
 */
@ApiTest
@ExtendWith(RuntimePerMethodExtension.class)
class ValidationIntegrationTest {

    private final InterceptorFunction methodFunctionMock = mock(InterceptorFunction.class);
    private final InterceptorFunction typeFunctionMock = mock(InterceptorFunction.class);
    private final InterceptorFunction globalFunctionMock = mock(InterceptorFunction.class);
    private int port;

    @BeforeEach
    void setup(RuntimeExtension extension) {
        port = getFreePort();
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(port),
                "web.http.path", "/api"
        ));

        try {
            when(methodFunctionMock.apply(any())).thenReturn(Result.success());
            when(typeFunctionMock.apply(any())).thenReturn(Result.success());
            when(globalFunctionMock.apply(any())).thenReturn(Result.success());
            var method = TestController.class.getDeclaredMethod("greeting");
            extension.registerSystemExtension(ServiceExtension.class, new RegistrationExtension(method, methodFunctionMock, GreetingDto.class, typeFunctionMock, globalFunctionMock));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void verifyControllerReachable() {
        var response = baseRequest()
                .get("/greeting")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertThat(response).isEqualTo("hello world!");
    }

    @Test
    void verifyMethodBoundFunctionIsInvoked() {
        when(methodFunctionMock.apply(any())).thenReturn(Result.success());
        baseRequest()
                .get("/greeting")
                .then()
                .statusCode(200)
                .extract().body().asString();

        verify(methodFunctionMock).apply(any());
    }

    @Test
    void verifyMethodBoundFunction_whenReturnsFailure() {
        when(methodFunctionMock.apply(any())).thenReturn(Result.failure("not valid!"));

        var response = baseRequest()
                .get("/greeting")
                .then()
                .statusCode(400)
                .extract().body().asString();

        assertThat(response).isEqualTo("[{\"message\":\"not valid!\",\"type\":\"InvalidRequest\",\"path\":null,\"invalidValue\":null}]");
        verify(methodFunctionMock).apply(any());
    }

    @Test
    void verifyTypeBoundFunction_whenSuccess() {
        var response = baseRequest()
                .contentType(ContentType.JSON)
                .body(new GreetingDto("max mustermann"))
                .post("/greeting")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertThat(response).isEqualTo("hello, max mustermann");
    }

    @Test
    void verifyTypeBoundFunction_whenFailure() {
        when(typeFunctionMock.apply(any())).thenReturn(Result.failure("type: not valid!"));

        var response = baseRequest()
                .contentType(ContentType.JSON)
                .body(new GreetingDto("max mustermann"))
                .post("/greeting")
                .then()
                .statusCode(400)
                .extract().body().asString();
        assertThat(response).isEqualTo("[{\"message\":\"type: not valid!\",\"type\":\"InvalidRequest\",\"path\":null,\"invalidValue\":null}]");
    }

    @Test
    void verifyGlobalFunction_whenFailure() {
        when(globalFunctionMock.apply(any())).thenReturn(Result.failure("global: not valid!"));

        var response = baseRequest()
                .contentType(ContentType.JSON)
                .body(new GreetingDto("max mustermann"))
                .post("/greeting")
                .then()
                .statusCode(400)
                .extract().body().asString();
        assertThat(response).isEqualTo("[{\"message\":\"global: not valid!\",\"type\":\"InvalidRequest\",\"path\":null,\"invalidValue\":null}]");
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost")
                .port(port)
                .basePath("/api")
                .when();
    }

}
