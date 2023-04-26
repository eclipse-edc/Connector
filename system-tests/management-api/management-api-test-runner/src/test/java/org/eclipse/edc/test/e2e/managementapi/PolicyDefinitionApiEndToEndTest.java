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
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;

@EndToEndTest
public class PolicyDefinitionApiEndToEndTest {

    public static final int PORT = getFreePort();

    @RegisterExtension
    static EdcRuntimeExtension controlPlane = new EdcRuntimeExtension(
            ":system-tests:management-api:management-api-test-runtime",
            "control-plane",
            new HashMap<>() {
                {
                    put("edc.ids.id", "urn:connector:" + UUID.randomUUID());
                    put("web.http.path", "/");
                    put("web.http.port", String.valueOf(getFreePort()));
                    put("web.http.management.path", "/management");
                    put("web.http.management.port", String.valueOf(PORT));
                }
            }
    );

    @Test // stub test just to verify structure
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
