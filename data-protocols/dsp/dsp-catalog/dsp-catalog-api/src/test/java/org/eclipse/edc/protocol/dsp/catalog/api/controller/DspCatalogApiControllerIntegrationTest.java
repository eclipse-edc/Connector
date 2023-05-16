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
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.connector.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.Namespaces;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.api.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApiTest
@ExtendWith(EdcExtension.class)
class DspCatalogApiControllerIntegrationTest {

    private static final String DSPACE_CATALOG_ERROR = Namespaces.DSPACE_SCHEMA + "CatalogError";

    private final int dspApiPort = getFreePort();
    private final String dspApiPath = "/api/v1/dsp";
    private final String callbackAddress = "http://callback";
    private final String authHeader = "auth";
    private final IdentityService identityService = mock(IdentityService.class);
    private final CatalogProtocolService service = mock(CatalogProtocolService.class);

    private JsonObject request;

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.protocol.port", String.valueOf(dspApiPort),
                "web.http.protocol.path", dspApiPath,
                "edc.dsp.callback.address", callbackAddress
        ));
        when(service.getCatalog(any(), any())).thenReturn(ServiceResult.success(createCatalog()));

        extension.registerServiceMock(IdentityService.class, identityService);
        extension.registerServiceMock(CatalogProtocolService.class, service);
        extension.registerServiceMock(DataServiceRegistry.class, mock(DataServiceRegistry.class));

        request = Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder()
                        .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                        .build())
                .add(TYPE, DSPACE_TYPE_CATALOG_REQUEST_MESSAGE)
                .build();
    }

    @Test
    void catalogRequest() {
        when(identityService.verifyJwtToken(any(), any()))
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().build()));

        baseRequest()
                .body(request)
                .contentType(MediaType.APPLICATION_JSON)
                .post(BASE_PATH + CATALOG_REQUEST)
                .then()
                .statusCode(200)
                .body(CONTEXT, notNullValue())
                .body(TYPE, is(DCAT_PREFIX + ":Catalog"));
    }

    @Test
    void catalogRequest_authenticationFailed_returnUnauthorized() {
        when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.failure("error"));

        var result = baseRequest()
                .body(request)
                .contentType(MediaType.APPLICATION_JSON)
                .post(BASE_PATH + CATALOG_REQUEST)
                .then()
                .statusCode(401)
                .extract().as(Map.class);

        assertThat(result.get(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_CATALOG_ERROR);
        assertThat(result.get(DSPACE_PROPERTY_CODE)).isEqualTo("401");
        assertThat(result.get(DSPACE_PROPERTY_REASON)).isNotNull();
    }

    @Test
    void catalogRequest_requestTransformationFailed_returnBadRequest() {
        when(identityService.verifyJwtToken(any(), any()))
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().build()));

        var invalidRequest = Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder()
                        .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                        .build())
                .add(TYPE, "not-a-catalog-request-message")
                .build();

        var result = baseRequest()
                .body(invalidRequest)
                .contentType(MediaType.APPLICATION_JSON)
                .post(BASE_PATH + CATALOG_REQUEST)
                .then()
                .statusCode(400)
                .extract().as(Map.class);

        assertThat(result.get(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_CATALOG_ERROR);
        assertThat(result.get(DSPACE_PROPERTY_CODE)).isEqualTo("400");
        assertThat(result.get(DSPACE_PROPERTY_REASON)).isNotNull();
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + dspApiPort)
                .basePath(dspApiPath)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .when();
    }

    private Catalog createCatalog() {
        return Catalog.Builder.newInstance()
                .datasets(emptyList())
                .dataServices(emptyList())
                .build();
    }
}
