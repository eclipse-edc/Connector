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

package org.eclipse.edc.connector.api.management.catalog;

import jakarta.json.Json;
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.catalog.spi.CatalogRequest;
import org.eclipse.edc.connector.api.management.catalog.model.CatalogRequestDto;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class CatalogApiControllerTest extends RestControllerTestBase {

    private final CatalogService service = mock(CatalogService.class);
    private final TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);
    private final JsonLd jsonLd = mock(JsonLd.class);

    @Override
    protected Object controller() {
        return new CatalogApiController(service, transformerRegistry, jsonLd);
    }

    @Test
    void requestCatalog() {
        var expanded = Json.createObjectBuilder().build();
        var dto = CatalogRequestDto.Builder.newInstance().providerUrl("http://url").build();
        var request = CatalogRequest.Builder.newInstance().providerUrl("http://url").build();
        when(jsonLd.expand(any())).thenReturn(Result.success(expanded));
        when(transformerRegistry.transform(any(), eq(CatalogRequestDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(any(), eq(CatalogRequest.class))).thenReturn(Result.success(request));
        when(service.request(any(), any(), any())).thenReturn(completedFuture("{}".getBytes()));

        var requestDto = CatalogRequestDto.Builder.newInstance()
                .protocol("protocol")
                .querySpec(QuerySpecDto.Builder.newInstance()
                        .limit(29)
                        .offset(13)
                        .filterExpression(List.of(TestFunctions.createCriterionDto("fooProp", "", "bar"), TestFunctions.createCriterionDto("bazProp", "in", List.of("blip", "blup", "blop"))))
                        .sortField("someField")
                        .sortOrder(SortOrder.DESC).build())
                .providerUrl("some.provider.url")

                .build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestDto)
                .post("/v2/catalog/request")
                .then()
                .statusCode(200)
                .contentType(JSON);
        verify(jsonLd).expand(any());
        verify(transformerRegistry).transform(expanded, CatalogRequestDto.class);
        verify(transformerRegistry).transform(dto, CatalogRequest.class);
    }

    @Test
    void catalogRequest_shouldReturnBadRequest_whenTransformFails() {
        var expanded = Json.createObjectBuilder().build();
        when(jsonLd.expand(any())).thenReturn(Result.success(expanded));
        when(transformerRegistry.transform(any(), eq(CatalogRequestDto.class))).thenReturn(Result.failure("error"));

        var requestDto = CatalogRequestDto.Builder.newInstance()
                .protocol("protocol")
                .querySpec(QuerySpecDto.Builder.newInstance()
                        .limit(29)
                        .offset(13)
                        .filterExpression(List.of(TestFunctions.createCriterionDto("fooProp", "", "bar"), TestFunctions.createCriterionDto("bazProp", "in", List.of("blip", "blup", "blop"))))
                        .sortField("someField")
                        .sortOrder(SortOrder.DESC).build())
                .providerUrl("some.provider.url")

                .build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestDto)
                .post("/v2/catalog/request")
                .then()
                .statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    void requestCatalog_shouldReturnBadRequest_whenNoBody() {
        given()
                .port(port)
                .contentType(JSON)
                .post("/v2/catalog/request")
                .then()
                .statusCode(400);
    }

    @Test
    void requestCatalog_shouldReturnBadGateway_whenServiceFails() {
        var expanded = Json.createObjectBuilder().build();
        var dto = CatalogRequestDto.Builder.newInstance().providerUrl("http://url").build();
        var request = CatalogRequest.Builder.newInstance().providerUrl("http://url").build();
        when(jsonLd.expand(any())).thenReturn(Result.success(expanded));
        when(transformerRegistry.transform(any(), eq(CatalogRequestDto.class))).thenReturn(Result.success(dto));
        when(transformerRegistry.transform(any(), eq(CatalogRequest.class))).thenReturn(Result.success(request));
        when(service.request(any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));

        var requestDto = CatalogRequestDto.Builder.newInstance()
                .protocol("protocol")
                .querySpec(QuerySpecDto.Builder.newInstance()
                        .limit(29)
                        .offset(13)
                        .filterExpression(List.of(TestFunctions.createCriterionDto("fooProp", "", "bar"), TestFunctions.createCriterionDto("bazProp", "in", List.of("blip", "blup", "blop"))))
                        .sortField("someField")
                        .sortOrder(SortOrder.DESC).build())
                .providerUrl("some.provider.url")

                .build();

        given()
                .port(port)
                .contentType(JSON)
                .body(requestDto)
                .post("/v2/catalog/request")
                .then()
                .statusCode(502);
    }

}
