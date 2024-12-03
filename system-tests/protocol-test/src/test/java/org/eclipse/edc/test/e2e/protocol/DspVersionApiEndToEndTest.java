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

package org.eclipse.edc.test.e2e.protocol;

import io.restassured.http.ContentType;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersion;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_PATH;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_PROTOCOL_VERSIONS;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_VERSION;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

@EndToEndTest
public class DspVersionApiEndToEndTest {

    private static final int PROTOCOL_PORT = Ports.getFreePort();
    @RegisterExtension
    static RuntimeExtension runtime = new RuntimePerClassExtension(new EmbeddedRuntime(
            "runtime",
            Map.of(
                    "web.http.protocol.path", "/protocol",
                    "web.http.protocol.port", String.valueOf(PROTOCOL_PORT)
            ),
            ":data-protocols:dsp:dsp-version:dsp-version-http-api",
            ":data-protocols:dsp:dsp-http-api-configuration",
            ":data-protocols:dsp:dsp-http-core",
            ":extensions:common:iam:iam-mock",
            ":core:control-plane:control-plane-aggregate-services",
            ":core:control-plane:control-plane-core",
            ":extensions:common:http"
    ));

    @Test
    void shouldReturnValidJson() {
        runtime.getService(ProtocolVersionRegistry.class)
                .register(new ProtocolVersion("1.0", "/v1/path"));

        var response = given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
                .header("Authorization", "{\"region\": \"any\", \"audience\": \"any\", \"clientId\":\"any\"}")
                .get("/.well-known/dspace-version")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("'@context'", nullValue())
                .extract().body().as(JsonObject.class);


        assertThat(response.getJsonArray(DSPACE_PROPERTY_PROTOCOL_VERSIONS))
                .hasSize(1)
                .extracting(JsonValue::asJsonObject)
                .first().satisfies(protocolVersion -> versionIs(protocolVersion, "1.0", "/v1/path"));

    }

    @Test
    void shouldReturnError_whenNotAuthorized() {

        var authorizationHeader = """
                {"region": "any", "audience": "any", "clientId":"faultyClientId"}"
                """;

        given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
                .header("Authorization", authorizationHeader)
                .get("/.well-known/dspace-version")
                .then()
                .log().ifError()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .body("code", equalTo("401"))
                .body("reason[0]", equalTo("Unauthorized"))
                .body("'@context'", nullValue());
    }

    private void versionIs(JsonObject protocolVersion, String version, String path) {
        assertThat(protocolVersion.getString(DSPACE_PROPERTY_VERSION)).isEqualTo(version);
        assertThat(protocolVersion.getString(DSPACE_PROPERTY_PATH)).isEqualTo(path);
    }
}
