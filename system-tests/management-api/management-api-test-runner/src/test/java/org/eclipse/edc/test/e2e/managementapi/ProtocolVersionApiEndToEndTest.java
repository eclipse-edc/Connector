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

package org.eclipse.edc.test.e2e.managementapi;

import jakarta.json.JsonObject;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Collectors;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.hamcrest.Matchers.greaterThan;

public class ProtocolVersionApiEndToEndTest {

    abstract static class Tests {

        @Test
        void requestProtocolVersions(ManagementEndToEndTestContext context, DataspaceProfileContextRegistry registry) {

            var supportedVersions = registry.getProtocolVersions().protocolVersions()
                    .stream().collect(Collectors.toMap(ProtocolVersion::version, ProtocolVersion::path));
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "ProtocolVersionRequest")
                    .add("counterPartyAddress", context.providerProtocolUrl())
                    .add("counterPartyId", "providerId")
                    .add("protocol", "dataspace-protocol-http")
                    .build();

            var response = context.baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v4alpha/protocol-versions/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("protocolVersions.size()", greaterThan(0))
                    .extract().body().as(JsonObject.class);

            response.getJsonArray("protocolVersions").forEach(it -> {
                var protocolVersion = it.asJsonObject();
                var version = protocolVersion.getString("version");
                var path = protocolVersion.getString("path");
                assertThat(supportedVersions.get(version)).isEqualTo(path);
            });
        }

    }

    @Nested
    @EndToEndTest
    @ExtendWith(ManagementEndToEndExtension.InMemory.class)
    class InMemory extends Tests {
    }

}
