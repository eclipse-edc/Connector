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

package org.eclipse.edc.protocol.dsp.transferprocess.api;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;

@ExtendWith(EdcExtension.class)
public class DspTransferProcessApiIntegrationTest {
    private final int port = getFreePort();

    private final String authKey = "123456";

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.management.port", String.valueOf(getFreePort()),
                "web.http.management.path", "/api/v1/management",
                "web.http.protocol.port", String.valueOf(port),
                "web.http.protocol.path", "/api/v1/dsp",
                "edc.api.auth.key", authKey,
                "edc.ids.id", "testID"
        ));
    }

    @Test
    public void getTransferProcess() {
        baseRequest()
                .get("/transfers/0")
                .then()
                .statusCode(200)
                .contentType("application/json");
    }

    @Test
    public void initiateTransferProcess() {
        baseRequest()
                .get("/transfers/request")
                .then()
                .statusCode(200)
                .contentType("application/json");
    }

    @Test
    public void consumerTransferProcessStart() {
        baseRequest()
                .get("/transfers/0/start")
                .then()
                .statusCode(200)
                .contentType("application/json");
    }

    @Test
    public void consumerTransferProcessCompletion() {
        baseRequest()
                .get("/transfers/0/completion")
                .then()
                .statusCode(200)
                .contentType("application/json");
    }

    @Test
    public void consumerTransferProcessTermination() {
        baseRequest()
                .get("/transfers/0/termination")
                .then()
                .statusCode(200)
                .contentType("application/json");
    }

    @Test
    public void consumerTransferProcessSuspension() {
        baseRequest()
                .get("/transfers/0/suspension")
                .then()
                .statusCode(200)
                .contentType("application/json");
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/api/v1/dsp")
                .when();
    }
}
