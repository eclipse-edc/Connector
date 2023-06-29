/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.api.controller;

import io.restassured.specification.RequestSpecification;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.connector.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_TYPE;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.DATASET_REQUEST;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_ERROR;
import static org.eclipse.edc.protocol.dsp.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON;
import static org.eclipse.edc.service.spi.result.ServiceResult.badRequest;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class DspCatalogApiControllerTest extends RestControllerTestBase {

    private final Monitor monitor = mock(Monitor.class);
    private final IdentityService identityService = mock(IdentityService.class);
    private final TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);
    private final CatalogProtocolService service = mock(CatalogProtocolService.class);
    private final String callbackAddress = "http://callback";
    private final JsonObject request = createObjectBuilder()
            .add(TYPE, DSPACE_TYPE_CATALOG_REQUEST_MESSAGE)
            .build();
    private final CatalogRequestMessage requestMessage = CatalogRequestMessage.Builder.newInstance()
            .protocol("protocol")
            .build();

    @Test
    void requestCatalog_returnCatalog() {
        var catalog = createObjectBuilder().add(JsonLdKeywords.TYPE, "catalog").build();

        var token = createToken();
        when(transformerRegistry.transform(isA(JsonObject.class), eq(CatalogRequestMessage.class))).thenReturn(Result.success(requestMessage));
        when(transformerRegistry.transform(any(Catalog.class), eq(JsonObject.class))).thenReturn(Result.success(catalog));
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress))).thenReturn(Result.success(token));
        when(service.getCatalog(any(), any())).thenReturn(ServiceResult.success(Catalog.Builder.newInstance().build()));

        baseRequest()
                .contentType(JSON)
                .body(request)
                .post(CATALOG_REQUEST)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(TYPE, is("catalog"));

        verify(service).getCatalog(requestMessage, token);

        // verify that the message protocol was set to the DSP protocol by the controller
        assertThat(requestMessage.getProtocol()).isEqualTo(DATASPACE_PROTOCOL_HTTP);
    }

    @Test
    void requestCatalog_invalidTypeInRequest_throwException() {
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.success(createToken()));

        var invalidRequest = createObjectBuilder()
                .add(TYPE, "not-a-catalog-request")
                .build();

        baseRequest()
                .contentType(JSON)
                .body(invalidRequest)
                .post(CATALOG_REQUEST)
                .then()
                .statusCode(400)
                .contentType(JSON)
                .body(TYPE, is(DSPACE_TYPE_CATALOG_ERROR))
                .body(format("'%s'", DSPACE_PROPERTY_CODE), is("400"))
                .body(format("'%s'", DSPACE_PROPERTY_REASON), notNullValue());
    }

    @Test
    void requestCatalog_transformingRequestFails_throwException() {
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.success(createToken()));
        when(transformerRegistry.transform(isA(JsonObject.class), eq(CatalogRequestMessage.class)))
                .thenReturn(Result.failure("error"));

        baseRequest()
                .contentType(JSON)
                .body(request)
                .post(CATALOG_REQUEST)
                .then()
                .statusCode(400)
                .contentType(JSON)
                .body(TYPE, is(DSPACE_TYPE_CATALOG_ERROR))
                .body(format("'%s'", DSPACE_PROPERTY_CODE), is("400"))
                .body(format("'%s'", DSPACE_PROPERTY_REASON), notNullValue());
    }

    @Test
    void requestCatalog_authenticationFails_throwException() {
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.failure("error"));

        baseRequest()
                .contentType(JSON)
                .body(request)
                .post(CATALOG_REQUEST)
                .then()
                .statusCode(401)
                .contentType(JSON)
                .body(TYPE, is(DSPACE_TYPE_CATALOG_ERROR))
                .body(format("'%s'", DSPACE_PROPERTY_CODE), is("401"))
                .body(format("'%s'", DSPACE_PROPERTY_REASON), notNullValue());
    }

    @Test
    void requestCatalog_shouldForwardServiceError_whenServiceCallFails() {
        when(service.getCatalog(any(), any())).thenReturn(badRequest("error"));
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.success(createToken()));
        when(transformerRegistry.transform(isA(JsonObject.class), eq(CatalogRequestMessage.class)))
                .thenReturn(Result.success(requestMessage));

        baseRequest()
                .contentType(JSON)
                .body(request)
                .post(CATALOG_REQUEST)
                .then()
                .statusCode(400)
                .contentType(JSON)
                .body(TYPE, is(DSPACE_TYPE_CATALOG_ERROR))
                .body(format("'%s'", DSPACE_PROPERTY_CODE), is("400"))
                .body(format("'%s'", DSPACE_PROPERTY_REASON), notNullValue());

        verify(service).getCatalog(any(), any());
    }

    @Test
    void getDataset_shouldGetDataset() {
        var claimToken = createToken();
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), any()))
                .thenReturn(Result.success(claimToken));
        var dataset = createDataset();
        when(service.getDataset(any(), any())).thenReturn(ServiceResult.success(dataset));
        var responseBody = createObjectBuilder().add(TYPE, DCAT_DATASET_TYPE).build();
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(responseBody));

        baseRequest()
                .get(DATASET_REQUEST + "/datasetId")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(TYPE, is(DCAT_DATASET_TYPE));

        verify(identityService).verifyJwtToken(argThat(it -> it.getToken().equals("auth")), eq(callbackAddress));
        verify(service).getDataset("datasetId", claimToken);
        verify(transformerRegistry).transform(dataset, JsonObject.class);
    }

    @Test
    void getDataset_shouldReturnUnauthorized_whenAuthorizationFails() {
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), any())).thenReturn(Result.failure("unauthorized"));

        baseRequest()
                .get(DATASET_REQUEST + "/datasetId")
                .then()
                .statusCode(401)
                .contentType(JSON)
                .body(TYPE, is(DSPACE_TYPE_CATALOG_ERROR))
                .body(format("'%s'", DSPACE_PROPERTY_CODE), is("401"))
                .body(format("'%s'", DSPACE_PROPERTY_REASON), notNullValue());

        verifyNoInteractions(service, transformerRegistry);
    }

    @Test
    void getDataset_shouldReturnNotFound_whenServiceReturnsNotFound() {
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), any()))
                .thenReturn(Result.success(createToken()));
        when(service.getDataset(any(), any())).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .get(DATASET_REQUEST + "/datasetId")
                .then()
                .statusCode(404)
                .contentType(JSON)
                .body(TYPE, is(DSPACE_TYPE_CATALOG_ERROR))
                .body(format("'%s'", DSPACE_PROPERTY_CODE), is("404"))
                .body(format("'%s'", DSPACE_PROPERTY_REASON), notNullValue());

        verifyNoInteractions(transformerRegistry);
    }

    @Test
    void getDataset_shouldReturnInternalServerError_whenTransformationFails() {
        var claimToken = createToken();
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), any()))
                .thenReturn(Result.success(claimToken));
        var dataset = createDataset();
        when(service.getDataset(any(), any())).thenReturn(ServiceResult.success(dataset));
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));

        baseRequest()
                .get(DATASET_REQUEST + "/datasetId")
                .then()
                .statusCode(500)
                .contentType(JSON)
                .body(TYPE, is(DSPACE_TYPE_CATALOG_ERROR))
                .body(format("'%s'", DSPACE_PROPERTY_CODE), is("500"))
                .body(format("'%s'", DSPACE_PROPERTY_REASON), notNullValue());
    }

    @Override
    protected Object controller() {
        return new DspCatalogApiController(monitor, identityService, transformerRegistry, callbackAddress, service);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath(BASE_PATH)
                .header(HttpHeaders.AUTHORIZATION, "auth")
                .when();
    }

    private Dataset createDataset() {
        var dataService = DataService.Builder.newInstance().build();
        var distribution = Distribution.Builder.newInstance().dataService(dataService).format("format").build();
        return Dataset.Builder.newInstance().distribution(distribution).offer("offerId", Policy.Builder.newInstance().build()).build();
    }

    private ClaimToken createToken() {
        return ClaimToken.Builder.newInstance().build();
    }
}
