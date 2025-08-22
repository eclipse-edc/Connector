/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.catalog;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
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
import static org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest.DATASET_REQUEST_TYPE;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
public abstract class BaseCatalogApiControllerTest extends RestControllerTestBase {

    protected final CatalogService service = mock();
    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final JsonObjectValidatorRegistry validatorRegistry = mock();

    @Test
    void requestCatalog() {
        var request = CatalogRequest.Builder.newInstance().counterPartyAddress("http://url").build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(), eq(CatalogRequest.class))).thenReturn(Result.success(request));
        when(service.requestCatalog(any(), any(), any(), any())).thenReturn(completedFuture(StatusResult.success("{}".getBytes())));
        var requestBody = Json.createObjectBuilder().add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl() + "/request")
                .then()
                .statusCode(200)
                .contentType(JSON);
        verify(transformerRegistry).transform(any(), eq(CatalogRequest.class));
    }

    @Test
    void requestCatalog_shouldReturnBadRequest_whenValidationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(Violation.violation("error", "path")));
        var requestBody = Json.createObjectBuilder().add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl() + "/request")
                .then()
                .statusCode(400);
        verify(validatorRegistry).validate(eq(CatalogRequest.CATALOG_REQUEST_TYPE), any());
        verifyNoInteractions(transformerRegistry, service);
    }

    @Test
    void requestCatalog_shouldReturnBadRequest_whenTransformFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(), eq(CatalogRequest.class))).thenReturn(Result.failure("error"));
        var requestBody = Json.createObjectBuilder().add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl() + "/request")
                .then()
                .statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    void requestCatalog_shouldReturnBadGateway_whenServiceFails() {
        var request = CatalogRequest.Builder.newInstance().counterPartyAddress("http://url").build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(), eq(CatalogRequest.class))).thenReturn(Result.success(request));
        when(service.requestCatalog(any(), any(), any(), any())).thenReturn(completedFuture(StatusResult.failure(FATAL_ERROR, "error")));

        var requestBody = Json.createObjectBuilder().add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl() + "/request")
                .then()
                .statusCode(502);
    }

    @Test
    void requestCatalog_shouldReturnBadGateway_whenServiceThrowsException() {
        var request = CatalogRequest.Builder.newInstance().counterPartyAddress("http://url").build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(), eq(CatalogRequest.class))).thenReturn(Result.success(request));
        when(service.requestCatalog(any(), any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        var requestBody = Json.createObjectBuilder().add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl() + "/request")
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
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(request));
        when(service.requestDataset(any(), any(), any(), any())).thenReturn(CompletableFuture.completedFuture(StatusResult.success("{}".getBytes())));
        var requestBody = Json.createObjectBuilder().add(DATASET_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl() + "/dataset/request")
                .then()
                .statusCode(200);

        verify(validatorRegistry).validate(eq(DATASET_REQUEST_TYPE), any());
        verify(transformerRegistry).transform(any(), eq(DatasetRequest.class));
        verify(service).requestDataset("dataset-id", "providerId", "http://provider-url", "protocol");
    }

    @Test
    void requestDataset_shouldReturnBadRequest_whenValidationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(Violation.violation("error", "path")));
        var requestBody = Json.createObjectBuilder().add(DATASET_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl() + "/dataset/request")
                .then()
                .statusCode(400);
        verify(validatorRegistry).validate(eq(DATASET_REQUEST_TYPE), any());
        verifyNoInteractions(transformerRegistry, service);
    }

    @Test
    void requestDataset_shouldReturnBadRequest_whenTransformFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(), eq(DatasetRequest.class))).thenReturn(Result.failure("error"));
        var requestBody = Json.createObjectBuilder().add(DATASET_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl() + "/dataset/request")
                .then()
                .statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    void requestDataset_shouldReturnBadGateway_whenServiceFails() {
        var request = DatasetRequest.Builder.newInstance().counterPartyAddress("http://url").build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(), eq(DatasetRequest.class))).thenReturn(Result.success(request));
        when(service.requestDataset(any(), any(), any(), any())).thenReturn(completedFuture(StatusResult.failure(FATAL_ERROR, "error")));

        var requestBody = Json.createObjectBuilder().add(DATASET_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl() + "/dataset/request")
                .then()
                .statusCode(502);
    }

    @Test
    void requestDataset_shouldReturnBadGateway_whenServiceThrowsException() {
        var request = DatasetRequest.Builder.newInstance().counterPartyAddress("http://url").build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(), eq(DatasetRequest.class))).thenReturn(Result.success(request));
        when(service.requestDataset(any(), any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        var requestBody = Json.createObjectBuilder().add(DATASET_REQUEST_PROTOCOL, "any").build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl() + "/dataset/request")
                .then()
                .statusCode(502);
    }

    @Test
    void requestCatalog_withAdditionalScopes() {
        var request = CatalogRequest.Builder.newInstance().counterPartyAddress("http://url").build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(argThat(o -> o instanceof JsonObject jo && jo.containsKey(CatalogRequest.CATALOG_REQUEST_ADDITIONAL_SCOPES)), eq(CatalogRequest.class))).thenReturn(Result.success(request));
        when(service.requestCatalog(any(), any(), any(), any())).thenReturn(completedFuture(StatusResult.success("{}".getBytes())));
        var requestBody = Json.createObjectBuilder()
                .add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, "any")
                .add(CatalogRequest.CATALOG_REQUEST_ADDITIONAL_SCOPES, Json.createArrayBuilder(List.of("scope1", "scope2")).build())
                .build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody)
                .post(baseUrl() + "/request")
                .then()
                .statusCode(200)
                .contentType(JSON);
        var captor = ArgumentCaptor.forClass(JsonObject.class);
        verify(transformerRegistry).transform(captor.capture(), eq(CatalogRequest.class));

        var jo = captor.getValue();
        assertThat(jo).containsKey(CatalogRequest.CATALOG_REQUEST_ADDITIONAL_SCOPES);
    }

    protected abstract String baseUrl();
}
