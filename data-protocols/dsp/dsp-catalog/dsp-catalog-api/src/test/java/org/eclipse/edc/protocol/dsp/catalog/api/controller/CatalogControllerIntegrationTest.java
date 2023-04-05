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
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.jsonld.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.JsonLdKeywords.TYPE;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.protocol.dsp.catalog.spi.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.spi.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCAT_PREFIX;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApiTest
@ExtendWith(EdcExtension.class)
class CatalogControllerIntegrationTest {

    private final int dspApiPort = getFreePort();
    private final String dspApiPath = "/api/v1/dsp";
    private String callbackAddress = "http://callback";
    private String authHeader = "auth";
    private IdentityService identityService = mock(IdentityService.class);
    
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
        extension.registerServiceMock(IdentityService.class, identityService);
        
        request = Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder()
                        .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                        .build())
                .add(TYPE, DSPACE_CATALOG_REQUEST_TYPE)
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
    
        baseRequest()
                .body(request)
                .contentType(MediaType.APPLICATION_JSON)
                .post(BASE_PATH + CATALOG_REQUEST)
                .then()
                .statusCode(401);
    }
    
    @Test
    void catalogRequest_requestTransformationFailed_returnBadRequest() {
        var invalidRequest = Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder()
                        .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                        .build())
                .add(TYPE, "not-a-catalog-request-message")
                .build();
        
        baseRequest()
                .body(invalidRequest)
                .contentType(MediaType.APPLICATION_JSON)
                .post(BASE_PATH + CATALOG_REQUEST)
                .then()
                .statusCode(400);
    }
    
    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + dspApiPort)
                .basePath(dspApiPath)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .when();
    }
}
