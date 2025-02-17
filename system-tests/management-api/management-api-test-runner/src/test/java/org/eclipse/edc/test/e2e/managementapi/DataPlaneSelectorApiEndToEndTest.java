/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.managementapi;

import io.restassured.http.ContentType;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class DataPlaneSelectorApiEndToEndTest {

    @Nested
    @EndToEndTest
    @ExtendWith(ManagementEndToEndExtension.InMemory.class)
    class InMemory extends Tests {
    }

    @Nested
    @PostgresqlIntegrationTest
    @ExtendWith(ManagementEndToEndExtension.Postgres.class)
    class Postgres extends Tests {
    }

    private abstract static class Tests {

        @Test
        void getAllDataPlaneInstancesV4(ManagementEndToEndTestContext context, DataPlaneInstanceStore store) {
            var instance = DataPlaneInstance.Builder.newInstance().url("http://localhost/any").build();
            store.save(instance);

            var responseBody = context.baseRequest()
                    .get("/v4alpha/dataplanes")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("size()", greaterThan(0))
                    .extract().body().jsonPath();

            var map = responseBody.getMap("find { it.id = '%s' }".formatted(instance.getId()));
            assertThat(map).isNotNull().satisfies(actual -> {
                assertThat(actual).containsEntry("url", instance.getUrl().toString());
                assertThat(actual).doesNotContainKeys("allowedDestTypes", "turnCount");
            });
        }

        @Test
        void getAllDataPlaneInstancesV3(ManagementEndToEndTestContext context, DataPlaneInstanceStore store) {
            var instance = DataPlaneInstance.Builder.newInstance().url("http://localhost/any").build();
            store.save(instance);

            var responseBody = context.baseRequest()
                    .get("/v3/dataplanes")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("size()", greaterThan(0))
                    .extract().body().jsonPath();

            var map = responseBody.getMap("find { it.id = '%s' }".formatted(instance.getId()));
            assertThat(map).isNotNull().satisfies(actual -> {
                assertThat(actual).containsEntry("url", instance.getUrl().toString());
                assertThat(actual).containsKeys("allowedDestTypes", "turnCount");
            });
        }
    }
}
