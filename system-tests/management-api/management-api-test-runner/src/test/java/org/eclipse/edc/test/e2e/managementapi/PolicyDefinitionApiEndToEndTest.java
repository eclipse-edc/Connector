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

package org.eclipse.edc.test.e2e.managementapi;

import io.restassured.http.ContentType;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@EndToEndTest
public class PolicyDefinitionApiEndToEndTest extends BaseManagementApiEndToEndTest {

    // stub test just to verify structure
    @Test
    void shouldReturnBadRequest() {
        given()
                .port(PORT)
                .basePath("/management")
                .body("{}")
                .contentType(ContentType.JSON)
                .post("/v2/policydefinitions")
                .then()
                .statusCode(400);
    }
}
