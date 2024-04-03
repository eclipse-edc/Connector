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
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_PATH;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_PROTOCOL_VERSIONS;
import static org.eclipse.edc.protocol.dsp.spi.type.DspVersionPropertyAndTypeNames.DSPACE_PROPERTY_VERSION;

@EndToEndTest
public class DspVersionApiEndToEndTest {

    private static final int PROTOCOL_PORT = Ports.getFreePort();
    private final JsonLd jsonLd = new TitaniumJsonLd(new ConsoleMonitor());

    @RegisterExtension
    static EdcRuntimeExtension runtime = new EdcRuntimeExtension(
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
    );

    @Test
    void shouldReturnValidJsonLd() {
        runtime.getContext().getService(ProtocolVersionRegistry.class)
                .register(new ProtocolVersion("1.0", "/v1/path"));

        var compacted = given()
                .port(PROTOCOL_PORT)
                .basePath("/protocol")
                .header("Authorization", "{\"region\": \"any\", \"audience\": \"any\", \"clientId\":\"any\"}")
                .get("/.well-known/dspace-version")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().body().as(JsonObject.class);

        var expansion = jsonLd.expand(compacted);

        assertThat(expansion).isSucceeded().satisfies(expanded -> {
            assertThat(expanded.getJsonArray(DSPACE_PROPERTY_PROTOCOL_VERSIONS)).hasSize(1).extracting(JsonValue::asJsonObject)
                    .first().satisfies(protocolVersion -> versionIs(protocolVersion, "1.0", "/v1/path"));
        });
    }

    private void versionIs(JsonObject protocolVersion, String version, String path) {
        assertThat(protocolVersion.getJsonArray(DSPACE_PROPERTY_VERSION)).hasSize(1).first()
                .extracting(JsonValue::asJsonObject).extracting(it -> it.getString(VALUE)).isEqualTo(version);
        assertThat(protocolVersion.getJsonArray(DSPACE_PROPERTY_PATH)).hasSize(1).first()
                .extracting(JsonValue::asJsonObject).extracting(it -> it.getString(VALUE)).isEqualTo(path);
    }
}
