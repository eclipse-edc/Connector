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

import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.DECLINED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.hamcrest.CoreMatchers.equalTo;

public abstract class AbstractEndToEndTransfer {

    protected final Duration timeout = Duration.ofSeconds(30);

    protected static final Participant CONSUMER = new Participant("consumer");
    protected static final Participant PROVIDER = new Participant("provider");

    @Test
    void httpPullDataTransfer() {
        registerDataPlanes();
        createResourcesOnProvider("asset-id", "HttpData", noConstraintPolicy());

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
        registerDataPlanes();
        createResourcesOnProvider("asset-id", "HttpProvision", noConstraintPolicy());

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
        registerDataPlanes();
        createResourcesOnProvider("asset-id", "HttpData", noConstraintPolicy());

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

    @Test
    void declinedContractRequestWhenPolicyIsNotEqualToTheOfferedOne() {
        registerDataPlanes();
        createResourcesOnProvider("asset-id", "HttpData", singleConstraintPolicy());

        var catalog = CONSUMER.getCatalog(PROVIDER.idsEndpoint());
        assertThat(catalog.getContractOffers()).hasSize(1);

        var contractOffer = catalog.getContractOffers().get(0);
        contractOffer.getPolicy().getPermissions().get(0).getConstraints().remove(0);
        var negotiationId = CONSUMER.negotiateContract(PROVIDER, contractOffer);

        await().untilAsserted(() -> {
            var state = CONSUMER.getContractNegotiationState(negotiationId);
            assertThat(state).isEqualTo(DECLINED.name());
        });
    }

    private void registerDataPlanes() {
        PROVIDER.registerDataPlane();
        CONSUMER.registerDataPlane();
    }

    private void createResourcesOnProvider(String assetId, String addressType, PolicyDefinition contractPolicy) {
        PROVIDER.createAsset(assetId, addressType);
        var accessPolicy = noConstraintPolicy();
        PROVIDER.createPolicy(accessPolicy);
        PROVIDER.createPolicy(contractPolicy);
        PROVIDER.createContractDefinition(assetId, "definitionId", accessPolicy.getUid(), contractPolicy.getUid());
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

    private PolicyDefinition singleConstraintPolicy() {
        return PolicyDefinition.Builder.newInstance()
                .policy(Policy.Builder.newInstance()
                        .permission(Permission.Builder.newInstance()
                                .constraint(AtomicConstraint.Builder.newInstance()
                                        .leftExpression(new LiteralExpression("any"))
                                        .operator(Operator.EQ)
                                        .rightExpression(new LiteralExpression("any"))
                                        .build())
                                .build())
                        .type(PolicyType.SET)
                        .build())
                .build();
    }
}
