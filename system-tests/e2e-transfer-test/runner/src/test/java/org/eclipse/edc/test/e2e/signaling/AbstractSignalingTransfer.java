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

package org.eclipse.edc.test.e2e.signaling;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.test.e2e.participant.EndToEndTransferParticipant;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public abstract class AbstractSignalingTransfer {

    protected static final EndToEndTransferParticipant CONSUMER = EndToEndTransferParticipant.Builder.newInstance()
            .name("consumer")
            .id("urn:connector:consumer")
            .build();
    protected static final EndToEndTransferParticipant PROVIDER = EndToEndTransferParticipant.Builder.newInstance()
            .name("provider")
            .id("urn:connector:provider")
            .build();
    protected final Duration timeout = Duration.ofSeconds(60);

    @Test
    @Disabled
    void httpPull_dataTransfer() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), httpDataAddressProperties());
        var dynamicReceiverProps = CONSUMER.dynamicReceiverPrivateProperties();

        var transferProcessId = CONSUMER.requestAsset(PROVIDER, assetId, dynamicReceiverProps, syncDataAddress(), "HttpData-PULL");
        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(STARTED.name());
        });

        // retrieve the data reference
        var edr = CONSUMER.getDataReference(transferProcessId);

        // pull the data without query parameter
        await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of(), equalTo("some information")));

        // pull the data with additional query parameter
        var msg = UUID.randomUUID().toString();
        await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of("message", msg), equalTo(msg)));

        // We expect two EDR because in the test runtime we have two receiver extensions static and dynamic one
        assertThat(CONSUMER.getAllDataReferences(transferProcessId)).hasSize(2);
    }

    @Test
    void httpPushDataTransfer() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), httpDataAddressProperties());
        var destination = httpDataAddress(CONSUMER.backendService() + "/api/consumer/store");

        var transferProcessId = CONSUMER.requestAsset(PROVIDER, assetId, noPrivateProperty(), destination, "HttpData-PUSH");
        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());

            given()
                    .baseUri(CONSUMER.backendService().toString())
                    .when()
                    .get("/api/consumer/data")
                    .then()
                    .statusCode(anyOf(is(200), is(204)))
                    .body(is(notNullValue()));
        });
    }

    private JsonObject httpDataAddress(String baseUrl) {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "baseUrl", baseUrl)
                        .build())
                .build();
    }

    private JsonObject syncDataAddress() {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpProxy")
                .build();
    }

    @NotNull
    private Map<String, Object> httpDataAddressOauth2Properties() {
        return Map.of(
                "name", "transfer-test",
                "baseUrl", PROVIDER.backendService() + "/api/provider/oauth2data",
                "type", "HttpData",
                "proxyQueryParams", "true",
                "oauth2:clientId", "clientId",
                "oauth2:clientSecretKey", "provision-oauth-secret",
                "oauth2:tokenUrl", PROVIDER.backendService() + "/api/oauth2/token"
        );
    }

    @NotNull
    private Map<String, Object> httpDataAddressProperties() {
        return Map.of(
                "name", "transfer-test",
                "baseUrl", PROVIDER.backendService() + "/api/provider/data",
                "type", "HttpData",
                "proxyQueryParams", "true"
        );
    }

    private void registerDataPlanes() {
        PROVIDER.registerDataPlane(Set.of("HttpData-PUSH", "HttpData-PULL"));
    }

    private void createResourcesOnProvider(String assetId, JsonObject contractPolicy, Map<String, Object> dataAddressProperties) {
        PROVIDER.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
        var accessPolicyId = PROVIDER.createPolicyDefinition(noConstraintPolicy());
        var contractPolicyId = PROVIDER.createPolicyDefinition(contractPolicy);
        PROVIDER.createContractDefinition(assetId, UUID.randomUUID().toString(), accessPolicyId, contractPolicyId);
    }

    private JsonObject noPrivateProperty() {
        return Json.createObjectBuilder().build();
    }
}
