/*
 *  Copyright (c) 2024 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial implementation
 *
 */

package org.eclipse.edc.test.e2e;

import io.restassured.http.ContentType;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;

@EndToEndTest
public class DataplaneSelectorControlApiEndToEndTest {

    private final int controlPort = getFreePort();
    private static final String DATA_PLANE_ID = UUID.randomUUID().toString();

    @RegisterExtension
    @Order(1)
    private final RuntimeExtension dataPlaneSelector = new RuntimePerMethodExtension(new EmbeddedRuntime(
            "data-plane-selector",
            ":core:common:connector-core",
            ":core:data-plane-selector:data-plane-selector-core",
            ":extensions:common:http",
            ":extensions:common:api:api-core",
            ":extensions:common:api:control-api-configuration",
            ":extensions:data-plane:data-plane-signaling:data-plane-signaling-client",
            ":extensions:data-plane-selector:data-plane-selector-control-api")
            .registerServiceMock(ProtocolWebhook.class, mock())
            .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                    "web.http.control.port", String.valueOf(controlPort),
                    "web.http.control.path", "/control",
                    "edc.transfer.proxy.token.verifier.publickey.alias", "public-key",
                    "edc.transfer.proxy.token.signer.privatekey.alias", "private-key"
            )))
    );

    @RegisterExtension
    @Order(2)
    private final RuntimeExtension dataPlane = new RuntimePerMethodExtension(new EmbeddedRuntime(
            "data-plane",
            ":dist:bom:dataplane-base-bom")
            .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                    "edc.runtime.id", DATA_PLANE_ID,
                    "web.http.port", String.valueOf(getFreePort()),
                    "web.http.path", "/api",
                    "web.http.control.port", String.valueOf(getFreePort()),
                    "web.http.control.path", "/control",
                    "edc.dpf.selector.url", String.format("http://localhost:%d/control/v1/dataplanes", controlPort),
                    "edc.transfer.proxy.token.verifier.publickey.alias", "public-key",
                    "edc.transfer.proxy.token.signer.privatekey.alias", "private-key"
            )))
    );

    @Test
    void shouldReturnSelfRegisteredDataplane() {
        var result = given()
                .basePath("/control")
                .port(controlPort)
                .when()
                .get("/v1/dataplanes")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().body().as(JsonArray.class);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldSelectDataPlane() {
        var requestBody = Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                .add("source", Json.createObjectBuilder()
                        .add("type", "HttpData"))
                .add("transferType", "HttpData-PUSH")
                .build();

        await().untilAsserted(() -> {
            given()
                    .basePath("/control")
                    .port(controlPort)
                    .when()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .post("/v1/dataplanes/select")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("@id", is(DATA_PLANE_ID));
        });
    }
}
