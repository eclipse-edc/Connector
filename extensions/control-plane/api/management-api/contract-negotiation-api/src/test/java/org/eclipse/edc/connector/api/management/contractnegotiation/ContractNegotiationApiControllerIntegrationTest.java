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

package org.eclipse.edc.connector.api.management.contractnegotiation;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.DECLINED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApiTest
@ExtendWith(EdcExtension.class)
class ContractNegotiationApiControllerIntegrationTest {

    private final int port = getFreePort();
    private final String authKey = "123456";
    private final RemoteMessageDispatcher dispatcher = mock(RemoteMessageDispatcher.class);
    private final String protocol = "protocol";

    @BeforeEach
    void setUp(EdcExtension extension) {
        when(dispatcher.protocol()).thenReturn("protocol");
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.management.port", String.valueOf(port),
                "web.http.management.path", "/api/v1/management",
                "edc.api.auth.key", authKey
        ));
    }

    @Test
    void getAllContractNegotiations(ContractNegotiationStore store) {
        store.updateOrCreate(createContractNegotiation("negotiationId"));

        baseRequest()
                .get("/contractnegotiations")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void getAllContractNegotiations_withPaging(ContractNegotiationStore store) {
        store.updateOrCreate(createContractNegotiation("negotiationId"));

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
    void queryAllContractNegotiations(ContractNegotiationStore store) {
        store.updateOrCreate(createContractNegotiation("negotiationId"));

        baseRequest()
                .contentType(JSON)
                .post("/contractnegotiations/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void queryAllContractNegotiations_withPaging(ContractNegotiationStore store) {
        store.updateOrCreate(createContractNegotiation("negotiationId"));

        baseRequest()
                .contentType(JSON)
                .body(QuerySpecDto.Builder.newInstance().offset(0).limit(15).sortOrder(SortOrder.ASC).build())
                .post("/contractnegotiations/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void queryAll_invalidQuery(ContractNegotiationStore store) {
        IntStream.range(0, 10).forEach(i -> store.updateOrCreate(createContractNegotiation("negotiationId" + i)));

        baseRequest()
                .contentType(JSON)
                .body(QuerySpecDto.Builder.newInstance().filterExpression(List.of(CriterionDto.from("id", "=", "negotiationId4"))).build())
                .post("/contractnegotiations/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void getSingleContractNegotation(ContractNegotiationStore store) {
        store.updateOrCreate(createContractNegotiation("negotiationId"));

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
        store.updateOrCreate(createContractNegotiationBuilder("negotiationId").state(REQUESTED.code()).build());

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
        store.updateOrCreate(createContractNegotiationBuilder("negotiationId").contractAgreement(contractAgreement).build());

        baseRequest()
                .get("/contractnegotiations/negotiationId/agreement")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", is(contractAgreement.getId()));
    }

    @Test
    void getSingleContractNegotationAgreement_notFound(ContractNegotiationStore store) {
        store.updateOrCreate(createContractNegotiation("negotiationId"));

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
                .offer(TestFunctions.createOffer())
                .build();

        baseRequest()
                .contentType(JSON)
                .body(request)
                .post("/contractnegotiations")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", not(emptyString()))
                .body("createdAt", not("0"));
    }

    @Test
    void initiateContractNegotiation_invalidBody(RemoteMessageDispatcherRegistry registry) {
        registry.register(new TestRemoteMessageDispatcher());
        var request = NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorId("connector")
                .protocol(TestRemoteMessageDispatcher.TEST_PROTOCOL)
                .connectorAddress(null) // breaks validation
                .offer(TestFunctions.createOffer())
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
        store.updateOrCreate(createContractNegotiation("negotiationId"));

        baseRequest()
                .contentType(JSON)
                .post("/contractnegotiations/negotiationId/cancel")
                .then()
                .statusCode(204);
    }

    @Test
    void decline(ContractNegotiationStore store, RemoteMessageDispatcherRegistry registry) {
        registry.register(dispatcher);
        when(dispatcher.send(any(), any())).thenReturn(completedFuture(null));
        var negotiation = createContractNegotiationBuilder("negotiationId")
                .state(REQUESTED.code())
                .correlationId(UUID.randomUUID().toString())
                .build();
        store.updateOrCreate(negotiation);

        baseRequest()
                .contentType(JSON)
                .post("/contractnegotiations/negotiationId/decline")
                .then()
                .statusCode(204);

        await().untilAsserted(() -> {
            assertThat(store.findById("negotiationId").getState()).isEqualTo(DECLINED.code());
        });
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/api/v1/management")
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
                .protocol(protocol);
    }

    private static class TestRemoteMessageDispatcher implements RemoteMessageDispatcher {
        static final String TEST_PROTOCOL = "test";

        @Override
        public String protocol() {
            return TEST_PROTOCOL;
        }

        @Override
        public <T, M extends RemoteMessage> CompletableFuture<T> send(Class<T> responseType, M message) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
