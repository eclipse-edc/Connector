/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.managementapi;

import io.restassured.common.mapper.TypeRef;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.test.e2e.managementapi.Runtimes.inMemoryRuntime;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Secret V1 endpoints end-to-end tests
 */
public class VersionApiEndToEndTest {

    abstract static class Tests extends ManagementApiEndToEndTestBase {

        Tests(EdcRuntimeExtension runtime) {
            super(runtime);
        }

        @Test
        void getVersion() {

            var result = given()
                    .port(7171)
                    .baseUri("http://localhost:%s/.well-known/api/v1/version".formatted(7171))
                    .when()
                    .get("/")
                    .then()
                    .statusCode(200)
                    .body(notNullValue())
                    .extract().body().as(new TypeRef<Map<String, List<VersionRecord>>>() {
                    });

            assertThat(result).containsKeys("management", "version", "control", "observability", "sts");
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        public static final EdcRuntimeExtension RUNTIME = inMemoryRuntime();

        InMemory() {
            super(RUNTIME);
        }

    }
}