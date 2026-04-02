/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.dataplane;

import io.restassured.http.ContentType;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;

import static io.restassured.RestAssured.given;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;

public class DataPlaneSignalingClient {
    private final ComponentRuntimeContext context;

    public DataPlaneSignalingClient(ComponentRuntimeContext context) {
        this.context = context;
    }

    public void awaitFlowToBe(String flowId, String status) {
        await().untilAsserted(() -> {
            var uri = context.getEndpoint("default").get();
            given()
                    .baseUri(uri.toString())
                    .get("/v1/dataflows/{flowId}/status", flowId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("state", equalTo(status));
        });
    }

    public void completePreparation(String flowId) {
        var uri = context.getEndpoint("default").get();

        given()
                .baseUri(uri.toString())
                .post("/control/flows/{flowId}/complete-preparation", flowId)
                .then()
                .log().ifValidationFails()
                .statusCode(204);
    }

    public void completeStartup(String flowId) {
        var uri = context.getEndpoint("default").get();

        given()
                .baseUri(uri.toString())
                .post("/control/flows/{flowId}/complete-startup", flowId)
                .then()
                .log().ifValidationFails()
                .statusCode(204);
    }

    public JsonObject getDataPlaneRegistrationMessage() {
        return getDataPlaneRegistrationMessage(null);
    }

    public void registerControlPlane(JsonObject message) {
        var uri = context.getEndpoint("default").get();

        given()
                .baseUri(uri.toString())
                .contentType(ContentType.JSON)
                .body(message.toString())
                .put("/v1/controlplanes/")
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

    public JsonObject getDataPlaneRegistrationMessage(JsonObject authorizationProfile) {
        var dataflowsEndpoint = context.getEndpoint("default").get() + "/v1/dataflows";

        var builder = createObjectBuilder()
                .add("dataplaneId", dataPlaneId())
                .add("endpoint", dataflowsEndpoint)
                .add("transferTypes", createArrayBuilder()
                        .add("Finite-PUSH")
                        .add("Finite-PULL")
                        .add("NonFinite-PUSH")
                        .add("NonFinite-PULL")
                        .add("AsyncPrepare-PUSH")
                        .add("AsyncStart-PULL")
                );

        if (authorizationProfile != null) {
            builder.add("authorization", Json.createArrayBuilder().add(authorizationProfile));
        }

        return builder.build();
    }

    public String dataPlaneId() {
        return context.getConfig().getString("dataplane.id");
    }
}
