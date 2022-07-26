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
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.hamcrest.CoreMatchers.equalTo;

public abstract class AbstractEndToEndTransfer {

    protected final Duration timeout = Duration.ofSeconds(30);

    protected static final Participant CONSUMER = new Participant("consumer");
    protected static final Participant PROVIDER = new Participant("provider");

    @Test
    void httpPullDataTransfer() {
        PROVIDER.registerDataPlane();
        CONSUMER.registerDataPlane();
        var definitionId = "1";
        createAssetAndContractDefinitionOnProvider("asset-id", definitionId, "HttpData");

        var catalog = CONSUMER.getCatalog(PROVIDER.idsEndpoint());
        assertThat(catalog.getContractOffers()).hasSize(1);

        var contractOffer = catalog.getContractOffers().get(0);
        var assetId = contractOffer.getAsset().getId();
        var negotiationId = CONSUMER.negotiateContract(PROVIDER, contractOffer);
        var contractAgreementId = CONSUMER.getContractAgreementId(negotiationId);

        assertThat(contractAgreementId).isNotEmpty();

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
        PROVIDER.registerDataPlane();
        CONSUMER.registerDataPlane();
        var definitionId = "1";
        createAssetAndContractDefinitionOnProvider("asset-id", definitionId, "HttpProvision");

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
        PROVIDER.registerDataPlane();
        var definitionId = "1";
        createAssetAndContractDefinitionOnProvider("asset-id", definitionId, "HttpData");

        var catalog = CONSUMER.getCatalog(PROVIDER.idsEndpoint());
        assertThat(catalog.getContractOffers()).hasSize(1);

        var contractOffer = catalog.getContractOffers().get(0);
        var assetId = contractOffer.getAsset().getId();
        var negotiationId = CONSUMER.negotiateContract(PROVIDER, contractOffer);
        var contractAgreementId = CONSUMER.getContractAgreementId(negotiationId);

        assertThat(contractAgreementId).isNotEmpty();

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

    private void createAssetAndContractDefinitionOnProvider(String assetId, String definitionId, String addressType) {
        PROVIDER.createAsset(assetId, addressType);
        var policyId = PROVIDER.createPolicy(assetId);
        PROVIDER.createContractDefinition(policyId, assetId, definitionId);
    }

    private DataAddress sync() {
        return DataAddress.Builder.newInstance().type("HttpProxy").build();
    }

}
