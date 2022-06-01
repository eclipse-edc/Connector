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

package org.eclipse.dataspaceconnector.test.e2e;

import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.ENDPOINT;
import static org.eclipse.dataspaceconnector.spi.types.domain.http.HttpDataAddressSchema.NAME;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.hamcrest.CoreMatchers.equalTo;

public abstract class AbstractEndToEndTransfer {

    protected final Duration timeout = Duration.ofSeconds(30);

    protected static final Participant CONSUMER = new Participant("consumer");
    protected static final Participant PROVIDER = new Participant("provider");

    @Test
    void httpPullDataTransfer() {
        String definitionId = "1";
        createAssetAndContractDefinitionOnProvider("asset-id", definitionId);

        await().atMost(timeout).untilAsserted(() -> {
            var catalog = CONSUMER.getCatalog(PROVIDER.idsEndpoint());
            assertThat(catalog.getContractOffers()).hasSize(1);
        });
        var catalog = CONSUMER.getCatalog(PROVIDER.idsEndpoint());

        var contractOffer = catalog.getContractOffers().get(0);
        var assetId = contractOffer.getAsset().getId();
        var negotiationId = CONSUMER.negotiateContract(PROVIDER, contractOffer);
        var contractAgreementId = CONSUMER.getContractAgreementId(negotiationId);

        assertThat(contractAgreementId).isNotEmpty();

        var transferProcessId = CONSUMER.dataRequest(contractAgreementId, assetId, PROVIDER, sync());

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
        });

        await().atMost(timeout).untilAsserted(() -> {
            given()
                    .baseUri(CONSUMER.backendService().toString())
                    .when()
                    .get("/api/service/providerData")
                    .then()
                    .statusCode(200)
                    .body("message", equalTo("some information"));
        });
    }

    @Test
    void httpPushDataTransfer() {
        PROVIDER.registerDataPlane();
        var definitionId = "1";
        createAssetAndContractDefinitionOnProvider("asset-id", definitionId);

        var catalog = CONSUMER.getCatalog(PROVIDER.idsEndpoint());
        assertThat(catalog.getContractOffers()).hasSize(1);

        var contractOffer = catalog.getContractOffers().get(0);
        var assetId = contractOffer.getAsset().getId();
        var negotiationId = CONSUMER.negotiateContract(PROVIDER, contractOffer);
        var contractAgreementId = CONSUMER.getContractAgreementId(negotiationId);

        assertThat(contractAgreementId).isNotEmpty();

        var destination = DataAddress.Builder.newInstance()
                .type("HttpData")
                .property(NAME, "data")
                .property(ENDPOINT, CONSUMER.backendService() + "/api/service")
                .build();
        var transferProcessId = CONSUMER.dataRequest(contractAgreementId, assetId, PROVIDER, destination);

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
        });

        await().atMost(timeout).untilAsserted(() -> {
            given()
                    .baseUri(CONSUMER.backendService().toString())
                    .when()
                    .get("/api/service/providerData")
                    .then()
                    .statusCode(200)
                    .body("message", equalTo("some information"));
        });
    }

    private void createAssetAndContractDefinitionOnProvider(String assetId, String definitionId) {
        PROVIDER.createAsset(assetId);
        var policyId = PROVIDER.createPolicy(assetId);
        PROVIDER.createContractDefinition(policyId, assetId, definitionId);
    }

    private DataAddress sync() {
        return DataAddress.Builder.newInstance().type("HttpProxy").build();
    }

}
