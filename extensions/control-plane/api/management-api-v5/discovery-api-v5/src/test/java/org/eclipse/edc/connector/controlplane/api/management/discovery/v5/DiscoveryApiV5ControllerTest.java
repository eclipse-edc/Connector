/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.discovery.v5;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryRequest;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryResponse;
import org.eclipse.edc.protocol.spi.discovery.DiscoveryService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
class DiscoveryApiV5ControllerTest extends RestControllerTestBase {

    private static final String PARTICIPANT_CONTEXT_ID = "test-pc";

    private final AuthorizationService authorizationService = mock();
    private final DiscoveryService discoveryService = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();

    private static String requestBody() {
        return Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", "https://w3id.org/edc/v0.0.1/ns/"))
                .add("@type", "DiscoveryRequest")
                .add("counterPartyAddress", "https://counter-party.example")
                .build()
                .toString();
    }

    @BeforeEach
    void setUp() {
        when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(ServiceResult.success());
    }

    @Test
    void shouldReturnDiscoveredProfiles() {
        var request = new DiscoveryRequest(null, "https://counter-party.example");
        when(transformerRegistry.transform(isA(JsonObject.class), eq(DiscoveryRequest.class))).thenReturn(Result.success(request));
        var match = new DiscoveryResponse("profile-1", "version", "/remote", "http");
        when(discoveryService.discover(eq(PARTICIPANT_CONTEXT_ID), eq(request))).thenReturn(ServiceResult.success(List.of(match)));
        when(transformerRegistry.transform(isA(DiscoveryResponse.class), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().add("profile", "profile-1").build()));

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody())
                .post(baseUrl() + "/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));

        verify(discoveryService).discover(PARTICIPANT_CONTEXT_ID, request);
    }

    @Test
    void shouldReturnBadRequestWhenRequestBodyIsMalformed() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(DiscoveryRequest.class)))
                .thenReturn(Result.failure("invalid"));

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody())
                .post(baseUrl() + "/request")
                .then()
                .statusCode(400);

        verify(discoveryService, never()).discover(any(), any());
    }

    @Test
    void shouldReturnBadRequestWhenDiscoveryFails() {
        var request = new DiscoveryRequest(null, "https://counter-party.example");
        when(transformerRegistry.transform(isA(JsonObject.class), eq(DiscoveryRequest.class))).thenReturn(Result.success(request));
        when(discoveryService.discover(eq(PARTICIPANT_CONTEXT_ID), eq(request)))
                .thenReturn(ServiceResult.badRequest("could not reach counter party"));

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody())
                .post(baseUrl() + "/request")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldReturnForbiddenWhenAuthorizationFails() {
        when(authorizationService.authorize(any(), eq(PARTICIPANT_CONTEXT_ID), any(), any()))
                .thenReturn(ServiceResult.unauthorized("nope"));

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody())
                .post(baseUrl() + "/request")
                .then()
                .statusCode(403);

        verify(discoveryService, never()).discover(any(), any());
    }

    @Override
    protected Object controller() {
        return new DiscoveryApiV5Controller(authorizationService, discoveryService, transformerRegistry, monitor);
    }

    private String baseUrl() {
        return "/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/discover";
    }
}
