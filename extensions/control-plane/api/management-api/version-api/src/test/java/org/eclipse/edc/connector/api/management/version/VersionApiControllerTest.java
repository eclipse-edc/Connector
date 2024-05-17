/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.version;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@ApiTest
class VersionApiControllerTest extends RestControllerTestBase {


    @BeforeEach
    void setup() {
    }

    @Test
    void getVersion() {
        var result = baseRequest()
                .get("/version")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().body().as(JsonObject.class);

        var version = result.get(EDC_NAMESPACE + "version");
        assertThat(version).isInstanceOf(JsonString.class);
        assertThat(((JsonString) version).getString()).hasToString("3.0.0");
    }

    @Override
    protected Object controller() {
        return new VersionApiController(Thread.currentThread().getContextClassLoader(), Json.createBuilderFactory(Map.of()), new ObjectMapper());
    }


    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .when();
    }
}
