/*
 *  Copyright (c) 2022 - 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation;

import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.TestFunctions.createOffer;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.REQUESTED;
import static org.hamcrest.Matchers.is;

@ExtendWith(EdcExtension.class)
class ContractNegotiationApiControllerIntegrationTest {

    private final int port = getFreePort();
    private final String authKey = "123456";

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.data.port", String.valueOf(port),
                "web.http.data.path", "/api/v1/data",
                "edc.api.auth.key", authKey
        ));
    }

    @Test
    void getAllContractNegotiations(ContractNegotiationStore store) {
        store.save(createContractNegotiation("negotiationId"));

        baseRequest()
                .get("/contractnegotiations")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void getAllContractNegotiations_withPaging(ContractNegotiationStore store) {
        store.save(createContractNegotiation("negotiationId"));

        baseRequest()
                .get("/contractnegotiations?offset=0&limit=15&sort=ASC")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void getAll_invalidQuery() {
        baseRequest()
                .get("/contractnegotiations?limit=1&offset=-1&filter=&sortField=")
                .then()
                .statusCode(400);
    }

    @Test
    void getSingleContractNegotation(ContractNegotiationStore store) {
        store.save(createContractNegotiation("negotiationId"));

        baseRequest()
                .get("/contractnegotiations/negotiationId")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", is("negotiationId"));
    }

    @Test
    void getSingleContractNegotation_notFound() {
        baseRequest()
                .get("/contractnegotiations/nonExistingId")
                .then()
                .statusCode(404);
    }

    @Test
    void getSingleContractNegotationState(ContractNegotiationStore store) {
        store.save(createContractNegotiationBuilder("negotiationId").state(REQUESTED.code()).build());

        var state = baseRequest()
                .get("/contractnegotiations/negotiationId/state")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().asString();

        assertThat(state).isEqualTo("{\"state\":\"REQUESTED\"}");
    }

    @Test
    void getSingleContractNegotationAgreement(ContractNegotiationStore store) {
        var contractAgreement = createContractAgreement("negotiationId");
        store.save(createContractNegotiationBuilder("negotiationId").contractAgreement(contractAgreement).build());

        baseRequest()
                .get("/contractnegotiations/negotiationId/agreement")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", is(contractAgreement.getId()));
    }

    @Test
    void getSingleContractNegotationAgreement_notFound(ContractNegotiationStore store) {
        store.save(createContractNegotiation("negotiationId"));

        baseRequest()
                .get("/contractnegotiations/negotiationId/agreement")
                .then()
                .statusCode(404);
    }

    @Test
    void initiateContractNegotiation(RemoteMessageDispatcherRegistry registry) {
        registry.register(new TestRemoteMessageDispatcher());
        var request = NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorId("connector")
                .protocol(TestRemoteMessageDispatcher.TEST_PROTOCOL)
                .connectorAddress("connectorAddress")
                .offer(createOffer())
                .build();

        var result = baseRequest()
                .contentType(JSON)
                .body(request)
                .post("/contractnegotiations")
                .then()
                .statusCode(200)
                .extract().body().asString();

        assertThat(result).isNotBlank();
    }

    @Test
    void initiateContractNegotiation_invalidBody(RemoteMessageDispatcherRegistry registry) {
        registry.register(new TestRemoteMessageDispatcher());
        var request = NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorId("connector")
                .protocol(TestRemoteMessageDispatcher.TEST_PROTOCOL)
                .connectorAddress(null) // breaks validation
                .offer(createOffer())
                .build();

        var result = baseRequest()
                .contentType(JSON)
                .body(request)
                .post("/contractnegotiations")
                .then()
                .statusCode(400)
                .extract().body().asString();

        assertThat(result).isNotBlank();
    }

    @Test
    void cancel(ContractNegotiationStore store) {
        store.save(createContractNegotiation("negotiationId"));

        baseRequest()
                .contentType(JSON)
                .post("/contractnegotiations/negotiationId/cancel")
                .then()
                .statusCode(204);
    }

    @Test
    void decline(ContractNegotiationStore store) {
        store.save(createContractNegotiationBuilder("negotiationId").state(REQUESTED.code()).build());

        baseRequest()
                .contentType(JSON)
                .post("/contractnegotiations/negotiationId/decline")
                .then()
                .statusCode(204);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/api/v1/data")
                .header("x-api-key", authKey)
                .when();
    }

    private ContractNegotiation createContractNegotiation(String negotiationId) {
        return createContractNegotiationBuilder(negotiationId)
                .build();
    }

    private ContractAgreement createContractAgreement(String negotiationId) {
        return ContractAgreement.Builder.newInstance()
                .id(negotiationId)
                .providerAgentId(UUID.randomUUID().toString())
                .consumerAgentId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private ContractNegotiation.Builder createContractNegotiationBuilder(String negotiationId) {
        return ContractNegotiation.Builder.newInstance()
                .id(negotiationId)
                .counterPartyId(UUID.randomUUID().toString())
                .counterPartyAddress("address")
                .protocol("protocol");
    }

    private static class TestRemoteMessageDispatcher implements RemoteMessageDispatcher {
        static final String TEST_PROTOCOL = "test";

        @Override
        public String protocol() {
            return TEST_PROTOCOL;
        }

        @Override
        public <T> CompletableFuture<T> send(Class<T> responseType, RemoteMessage message, MessageContext context) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
