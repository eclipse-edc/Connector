/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.test.e2e;

import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.hamcrest.CoreMatchers.equalTo;

public abstract class AbstractEndToEndTransfer {

    protected final Duration timeout = Duration.ofSeconds(60);

    protected static final Participant CONSUMER = new Participant("consumer");
    protected static final Participant PROVIDER = new Participant("provider");

    @Test
    void httpPullDataTransfer() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), UUID.randomUUID().toString(), httpDataAddressProperties());

        var catalog = CONSUMER.getCatalog(PROVIDER.idsEndpoint());
        assertThat(catalog.getContractOffers()).hasSizeGreaterThan(0);

        var contractOffer = catalog
                .getContractOffers()
                .stream()
                .filter(o -> o.getAsset().getId().equals(assetId))
                .findFirst()
                .get();
        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractOffer);

        var dataRequestId = UUID.randomUUID().toString();
        var transferProcessId = CONSUMER.dataRequest(dataRequestId, contractAgreementId, assetId, PROVIDER, sync());

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
        });

        // retrieve the data reference
        var edr = CONSUMER.getDataReference(dataRequestId);

        // pull the data without query parameter
        await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of(), equalTo("some information")));

        // pull the data with additional query parameter
        var msg = UUID.randomUUID().toString();
        await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of("message", msg), equalTo(msg)));
    }

    @Test
    void httpPullDataTransferProvisioner() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), UUID.randomUUID().toString(), Map.of(
                "name", "transfer-test",
                "baseUrl", PROVIDER.backendService() + "/api/provider/data",
                "type", "HttpProvision",
                "proxyQueryParams", "true"
        ));

        await().atMost(timeout).untilAsserted(() -> {
            var catalog = CONSUMER.getCatalog(PROVIDER.idsEndpoint());
            assertThat(catalog.getContractOffers()).hasSizeGreaterThan(0);
        });
        var catalog = CONSUMER.getCatalog(PROVIDER.idsEndpoint());

        var contractOffer = catalog
                .getContractOffers()
                .stream()
                .filter(o -> o.getAsset().getId().equals(assetId))
                .findFirst()
                .get();
        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractOffer);

        var dataRequestId = UUID.randomUUID().toString();
        var transferProcessId = CONSUMER.dataRequest(dataRequestId, contractAgreementId, assetId, PROVIDER, sync());

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
        });

        var edr = CONSUMER.getDataReference(dataRequestId);
        await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of(), equalTo("some information")));
    }

    @Test
    void httpPushDataTransfer() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), UUID.randomUUID().toString(), httpDataAddressProperties());

        var catalog = CONSUMER.getCatalog(PROVIDER.idsEndpoint());
        assertThat(catalog.getContractOffers()).hasSizeGreaterThan(0);

        var contractOffer = catalog
                .getContractOffers()
                .stream()
                .filter(o -> o.getAsset().getId().equals(assetId))
                .findFirst()
                .get();
        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractOffer);

        var destination = HttpDataAddress.Builder.newInstance()
                .baseUrl(CONSUMER.backendService() + "/api/consumer/store")
                .build();
        var transferProcessId = CONSUMER.dataRequest(UUID.randomUUID().toString(), contractAgreementId, assetId, PROVIDER, destination);

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
        });

        await().atMost(timeout).untilAsserted(() -> {
            given()
                    .baseUri(CONSUMER.backendService().toString())
                    .when()
                    .get("/api/consumer/data")
                    .then()
                    .statusCode(200)
                    .body("message", equalTo("some information"));
        });
    }

    @Test
    @DisplayName("Provider pushes data to Consumer, Provider needs to authenticate the data request through an oauth2 server")
    void httpPushDataTransfer_oauth2Provisioning() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), UUID.randomUUID().toString(), httpDataAddressOauth2Properties());

        var catalog = CONSUMER.getCatalog(PROVIDER.idsEndpoint());
        assertThat(catalog.getContractOffers()).hasSizeGreaterThan(0);

        var contractOffer = catalog
                .getContractOffers()
                .stream()
                .filter(o -> o.getAsset().getId().equals(assetId))
                .findFirst()
                .get();
        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractOffer);

        var destination = HttpDataAddress.Builder.newInstance()
                .baseUrl(CONSUMER.backendService() + "/api/consumer/store")
                .build();
        var transferProcessId = CONSUMER.dataRequest(UUID.randomUUID().toString(), contractAgreementId, assetId, PROVIDER, destination);

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
        });

        await().atMost(timeout).untilAsserted(() -> {
            given()
                    .baseUri(CONSUMER.backendService().toString())
                    .when()
                    .get("/api/consumer/data")
                    .then()
                    .statusCode(200)
                    .body("message", equalTo("some information"));
        });
    }

    @NotNull
    private static Map<String, String> httpDataAddressProperties() {
        return Map.of(
                "name", "transfer-test",
                "baseUrl", PROVIDER.backendService() + "/api/provider/data",
                "type", "HttpData",
                "proxyQueryParams", "true"
        );
    }

    @NotNull
    private static Map<String, String> httpDataAddressOauth2Properties() {
        return Map.of(
                "name", "transfer-test",
                "baseUrl", PROVIDER.backendService() + "/api/provider/oauth2data",
                "type", "HttpData",
                "authKey", "Authorization",
                "proxyQueryParams", "true",
                "oauth2:clientId", "clientId",
                "oauth2:clientSecret", "clientSecret",
                "oauth2:tokenUrl", PROVIDER.backendService() + "/api/oauth2/token"
        );
    }

    private void registerDataPlanes() {
        PROVIDER.registerDataPlane();
        CONSUMER.registerDataPlane();
    }

    private void createResourcesOnProvider(String assetId, PolicyDefinition contractPolicy, String definitionId, Map<String, String> dataAddressProperties) {
        PROVIDER.createAsset(assetId, dataAddressProperties);
        var accessPolicy = noConstraintPolicy();
        PROVIDER.createPolicy(accessPolicy);
        PROVIDER.createPolicy(contractPolicy);
        PROVIDER.createContractDefinition(assetId, definitionId, accessPolicy.getUid(), contractPolicy.getUid());
    }

    private DataAddress sync() {
        return DataAddress.Builder.newInstance().type("HttpProxy").build();
    }

    private PolicyDefinition noConstraintPolicy() {
        return PolicyDefinition.Builder.newInstance()
                .policy(Policy.Builder.newInstance()
                        .permission(Permission.Builder.newInstance()
                                .action(Action.Builder.newInstance().type("USE").build())
                                .build())
                        .type(PolicyType.SET)
                        .build())
                .build();
    }

}
