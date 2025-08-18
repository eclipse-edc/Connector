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
 *       Cofinity-X - unauthenticated version endpoint
 *
 */

package org.eclipse.edc.test.e2e.protocol;

import io.restassured.http.ContentType;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_BINDING;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_PATH;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_PROTOCOL_VERSIONS;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_VERSION;
import static org.hamcrest.CoreMatchers.nullValue;

@EndToEndTest
class DspVersionApiEndToEndTest {

    private static final int PROTOCOL_PORT = Ports.getFreePort();
    @RegisterExtension
    static RuntimeExtension runtime = new RuntimePerClassExtension(DspRuntime.createRuntimeWith(
            PROTOCOL_PORT,
            ":data-protocols:dsp:dsp-version:dsp-version-http-api"
    ));

    @Test
    void shouldReturnValidJson() {
        runtime.getService(DataspaceProfileContextRegistry.class)
                .register(new DataspaceProfileContext("profile", new ProtocolVersion("1.0", "/v1/path", "HTTPS"), () -> "url", "participantId", ct -> "id"));

        var response = given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
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
                .first().satisfies(protocolVersion -> versionIs(protocolVersion, "1.0", "/v1/path", "HTTPS"));

    }

    @Test
    void shouldIgnoreAuthorizationHeader() {

        var authorizationHeader = """
                {"region": "any", "audience": "any", "clientId":"faultyClientId"}"
                """;

        var response = given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
                .header("Authorization", authorizationHeader)
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
                .first().satisfies(protocolVersion -> versionIs(protocolVersion, "1.0", "/v1/path", "HTTPS"));
    }

    private void versionIs(JsonObject protocolVersion, String version, String path, String binding) {
        assertThat(protocolVersion.getString(DSPACE_PROPERTY_VERSION)).isEqualTo(version);
        assertThat(protocolVersion.getString(DSPACE_PROPERTY_PATH)).isEqualTo(path);
        assertThat(protocolVersion.getString(DSPACE_PROPERTY_BINDING)).isEqualTo(binding);
    }
}
