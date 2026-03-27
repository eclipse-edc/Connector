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

package org.eclipse.edc.connector.controlplane.api.management.catalog;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest.DATASET_REQUEST_PROTOCOL;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ApiTest
public abstract class BaseCatalogApiControllerTest extends RestControllerTestBase {

    protected final CatalogService service = mock();
    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final AuthorizationService authorizationService = mock();
    protected final ParticipantContextService participantContextService = mock();
    protected final String participantContextId = "test-participant-context-id";

    @BeforeEach
    void setup() {
        when(participantContextService.getParticipantContext(eq(participantContextId)))
                .thenReturn(ServiceResult.success(ParticipantContext.Builder.newInstance().participantContextId(participantContextId)
                        .identity(participantContextId).build()));

        when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(ServiceResult.success());
    }

    @Test
    void requestCatalog() {
        var request = CatalogRequest.Builder.newInstance().counterPartyAddress("http://url").build();
        when(transformerRegistry.transform(any(), eq(CatalogRequest.class))).thenReturn(Result.success(request));
        when(service.requestCatalog(any(), any(), any(), any(), any())).thenReturn(completedFuture(StatusResult.success("{}".getBytes())));
        var requestBody = Json.createObjectBuilder().add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl(participantContextId) + "/request")
                .then()
                .statusCode(200)
                .contentType(JSON);
        verify(transformerRegistry).transform(any(), eq(CatalogRequest.class));
    }

    void requestCatalog_authServiceFails() {
        var requestBody = Json.createObjectBuilder().add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, "any").build().toString();
        when(authorizationService.authorize(any(), eq(participantContextId), any(), any())).thenReturn(ServiceResult.unauthorized("unauthorized"));
        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl(participantContextId) + "/request")
                .then()
                .statusCode(200)
                .contentType(JSON);
        verifyNoMoreInteractions(service, authorizationService, participantContextService);
    }

    @Test
    void requestCatalog_shouldReturnBadRequest_whenTransformFails() {
        when(transformerRegistry.transform(any(), eq(CatalogRequest.class))).thenReturn(Result.failure("error"));
        var requestBody = Json.createObjectBuilder().add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl(participantContextId) + "/request")
                .then()
                .statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    void requestCatalog_shouldReturnBadGateway_whenServiceFails() {
        var request = CatalogRequest.Builder.newInstance().counterPartyAddress("http://url").build();
        when(transformerRegistry.transform(any(), eq(CatalogRequest.class))).thenReturn(Result.success(request));
        when(service.requestCatalog(any(), any(), any(), any(), any())).thenReturn(completedFuture(StatusResult.failure(FATAL_ERROR, "error")));

        var requestBody = Json.createObjectBuilder().add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl(participantContextId) + "/request")
                .then()
                .statusCode(502);
    }

    @Test
    void requestCatalog_shouldReturnBadGateway_whenServiceThrowsException() {
        var request = CatalogRequest.Builder.newInstance().counterPartyAddress("http://url").build();
        when(transformerRegistry.transform(any(), eq(CatalogRequest.class))).thenReturn(Result.success(request));
        when(service.requestCatalog(any(), any(), any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        var requestBody = Json.createObjectBuilder().add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl(participantContextId) + "/request")
                .then()
                .statusCode(502);
    }

    @Test
    void requestDataset_shouldCallService() {
        var request = DatasetRequest.Builder.newInstance()
                .id("dataset-id")
                .protocol("protocol")
                .counterPartyAddress("http://provider-url")
                .counterPartyId("providerId")
                .build();
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(request));
        when(service.requestDataset(any(), any(), any(), any(), any())).thenReturn(CompletableFuture.completedFuture(StatusResult.success("{}".getBytes())));
        var requestBody = Json.createObjectBuilder().add(DATASET_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl(participantContextId) + "/dataset/request")
                .then()
                .statusCode(200);

        verify(transformerRegistry).transform(any(), eq(DatasetRequest.class));
        verify(service).requestDataset(isA(ParticipantContext.class), eq("dataset-id"), eq("providerId"), eq("http://provider-url"), eq("protocol"));
    }

    void requestDataset_authServiceFails() {
        when(authorizationService.authorize(any(), eq(participantContextId), any(), any())).thenReturn(ServiceResult.unauthorized("unauthorized"));
        var requestBody = Json.createObjectBuilder().add(DATASET_REQUEST_PROTOCOL, "any").build().toString();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl(participantContextId) + "/dataset/request")
                .then()
                .statusCode(403);

        verifyNoMoreInteractions(service, authorizationService, participantContextService);
    }

    @Test
    void requestDataset_shouldReturnBadRequest_whenTransformFails() {
        when(transformerRegistry.transform(any(), eq(DatasetRequest.class))).thenReturn(Result.failure("error"));
        var requestBody = Json.createObjectBuilder().add(DATASET_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl(participantContextId) + "/dataset/request")
                .then()
                .statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    void requestDataset_shouldReturnBadGateway_whenServiceFails() {
        var request = DatasetRequest.Builder.newInstance().counterPartyAddress("http://url").build();
        when(transformerRegistry.transform(any(), eq(DatasetRequest.class))).thenReturn(Result.success(request));
        when(service.requestDataset(any(), any(), any(), any(), any())).thenReturn(completedFuture(StatusResult.failure(FATAL_ERROR, "error")));

        var requestBody = Json.createObjectBuilder().add(DATASET_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl(participantContextId) + "/dataset/request")
                .then()
                .statusCode(502);
    }

    @Test
    void requestDataset_shouldReturnBadGateway_whenServiceThrowsException() {
        var request = DatasetRequest.Builder.newInstance().counterPartyAddress("http://url").build();
        when(transformerRegistry.transform(any(), eq(DatasetRequest.class))).thenReturn(Result.success(request));
        when(service.requestDataset(any(), any(), any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        var requestBody = Json.createObjectBuilder().add(DATASET_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl(participantContextId) + "/dataset/request")
                .then()
                .statusCode(502);
    }

    @Test
    void requestCatalog_withAdditionalScopes() {
        var request = CatalogRequest.Builder.newInstance().counterPartyAddress("http://url").build();
        when(transformerRegistry.transform(argThat(o -> o instanceof JsonObject jo && jo.containsKey(CatalogRequest.CATALOG_REQUEST_ADDITIONAL_SCOPES)), eq(CatalogRequest.class))).thenReturn(Result.success(request));
        when(service.requestCatalog(any(), any(), any(), any(), any())).thenReturn(completedFuture(StatusResult.success("{}".getBytes())));
        var requestBody = Json.createObjectBuilder()
                .add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, "any")
                .add(CatalogRequest.CATALOG_REQUEST_ADDITIONAL_SCOPES, Json.createArrayBuilder(List.of("scope1", "scope2")).build())
                .build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl(participantContextId) + "/request")
                .then()
                .statusCode(200)
                .contentType(JSON);
        var captor = ArgumentCaptor.forClass(JsonObject.class);
        verify(transformerRegistry).transform(captor.capture(), eq(CatalogRequest.class));

        var jo = captor.getValue();
        assertThat(jo).containsKey(CatalogRequest.CATALOG_REQUEST_ADDITIONAL_SCOPES);
    }

    protected abstract String baseUrl(String participantContextId);
}
