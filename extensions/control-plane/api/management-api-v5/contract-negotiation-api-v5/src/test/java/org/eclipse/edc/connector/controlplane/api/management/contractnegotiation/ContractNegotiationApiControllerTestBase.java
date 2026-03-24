/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.NegotiationState;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.eclipse.edc.api.model.IdResponse.ID_RESPONSE_TYPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.NegotiationState.NEGOTIATION_STATE_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public abstract class ContractNegotiationApiControllerTestBase extends RestControllerTestBase {
    protected final ContractNegotiationService service = mock();
    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final AuthorizationService authorizationService = mock();
    protected final ParticipantContextService participantContextService = mock();
    private final String participantContextId = "test-participant-context-id";

    @BeforeEach
    void setup() {
        when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(ServiceResult.success());
    }

    private RequestSpecification baseRequest(String participantContextId) {
        return given()
                .baseUri("http://localhost:" + port + "/" + versionPath() + "/participants/" + participantContextId)
                .when();
    }

    protected abstract String versionPath();

    private ContractNegotiation createContractNegotiation(String negotiationId) {
        return createContractNegotiationBuilder(negotiationId)
                .build();
    }

    private ContractAgreement createContractAgreement(String negotiationId) {
        return ContractAgreement.Builder.newInstance()
                .id(negotiationId)
                .consumerId("test-consumer")
                .providerId("test-provider")
                .assetId(randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private ContractNegotiation.Builder createContractNegotiationBuilder(String negotiationId) {
        return ContractNegotiation.Builder.newInstance()
                .id(negotiationId)
                .counterPartyId(randomUUID().toString())
                .counterPartyAddress("address")
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance()
                        .uri("local://test")
                        .build()))
                .protocol("protocol");
    }

    @Nested
    class Delete {

        @Test
        void delete_shouldCallService() {
            when(service.delete(any())).thenReturn(ServiceResult.success());

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .delete("/contractnegotiations/cn1")
                    .then()
                    .statusCode(204);

            verify(service).delete("cn1");
        }

        @Test
        void delete_shouldFailDueToWrongState() {
            when(service.delete(any())).thenReturn(ServiceResult.conflict(format("Cannot delete negotiation in state: %s".formatted(ContractNegotiationStates.AGREED.name()))));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .delete("/contractnegotiations/cn1")
                    .then()
                    .statusCode(409);

            verify(service).delete("cn1");
        }

        @Test
        void delete_shouldFailDueToNegotiationNotFound() {
            when(service.delete(any())).thenReturn(ServiceResult.notFound("ContractNegotiation negotiationId not found"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .delete("/contractnegotiations/cn1")
                    .then()
                    .statusCode(404);

            verify(service).delete("cn1");
        }
    }

    @Nested
    class Terminate {

        @Test
        void terminate_shouldCallService() {
            var command = new TerminateNegotiationCommand("id", "reason");
            when(transformerRegistry.transform(any(JsonObject.class), eq(TerminateNegotiationCommand.class))).thenReturn(Result.success(command));
            when(service.terminate(any())).thenReturn(ServiceResult.success());

            baseRequest(participantContextId)
                    .body(Json.createObjectBuilder().add(ID, "id").build())
                    .contentType(JSON)
                    .post("/contractnegotiations/cn1/terminate")
                    .then()
                    .statusCode(204);

            verify(service).terminate(command);
        }

        @Test
        void terminate_shouldReturnBadRequest_whenTransformerFails() {
            when(transformerRegistry.transform(any(JsonObject.class), eq(TerminateNegotiationCommand.class))).thenReturn(Result.failure("error"));

            baseRequest(participantContextId)
                    .body(Json.createObjectBuilder().add(ID, "id").build())
                    .contentType(JSON)
                    .post("/contractnegotiations/cn1/terminate")
                    .then()
                    .statusCode(400);

            verifyNoInteractions(service);
        }

        @Test
        void terminate_shouldReturnError_whenServiceFails() {
            var command = new TerminateNegotiationCommand("id", "reason");
            when(transformerRegistry.transform(any(JsonObject.class), eq(TerminateNegotiationCommand.class))).thenReturn(Result.success(command));
            when(service.terminate(any())).thenReturn(ServiceResult.conflict("conflict"));

            baseRequest(participantContextId)
                    .body(Json.createObjectBuilder().add(ID, "id").build())
                    .contentType(JSON)
                    .post("/contractnegotiations/cn1/terminate")
                    .then()
                    .statusCode(409);
        }

    }

    @Nested
    class Initiate {

        @Test
        void initiate() {
            var contractNegotiation = createContractNegotiation("cn1");
            var responseBody = createObjectBuilder().add(TYPE, ID_RESPONSE_TYPE).add(ID, contractNegotiation.getId()).build();

            when(transformerRegistry.transform(any(JsonObject.class), eq(ContractRequest.class))).thenReturn(Result.success(
                    ContractRequest.Builder.newInstance()
                            .protocol("test-protocol")
                            .counterPartyAddress("test-cb")
                            .contractOffer(ContractOffer.Builder.newInstance()
                                    .id("test-offer-id")
                                    .assetId(randomUUID().toString())
                                    .policy(Policy.Builder.newInstance().build())
                                    .build())
                            .build()));

            when(participantContextService.getParticipantContext(any()))
                    .thenReturn(ServiceResult.success(ParticipantContext.Builder.newInstance().participantContextId(participantContextId).identity(participantContextId).build()));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(responseBody));
            when(service.initiateNegotiation(any(ParticipantContext.class), any(ContractRequest.class))).thenReturn(ServiceResult.success(contractNegotiation));

            when(transformerRegistry.transform(any(IdResponse.class), eq(JsonObject.class))).thenReturn(Result.success(responseBody));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(createObjectBuilder().build())
                    .post("/contractnegotiations")
                    .then()
                    .statusCode(200)
                    .body(ID, is(contractNegotiation.getId()));

            verify(service).initiateNegotiation(any(), any());
            verify(transformerRegistry).transform(any(JsonObject.class), eq(ContractRequest.class));
            verify(transformerRegistry).transform(any(IdResponse.class), eq(JsonObject.class));
            verifyNoMoreInteractions(transformerRegistry, service);
        }

        @Test
        void initiate_invalidRequest() {
            when(transformerRegistry.transform(any(JsonObject.class), any())).thenReturn(Result.failure("test-failure"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(createObjectBuilder().build())
                    .post("/contractnegotiations")
                    .then()
                    .statusCode(400);
            verifyNoMoreInteractions(service);
        }
    }

    @Nested
    class FindAgreement {

        @Test
        void getAgreement() {
            when(service.getForNegotiation(eq("cn1"))).thenReturn(createContractAgreement("cn1"));
            when(transformerRegistry.transform(any(ContractAgreement.class), eq(JsonObject.class)))
                    .thenReturn(Result.success(createObjectBuilder().build()));

            baseRequest(participantContextId)
                    .get("/contractnegotiations/cn1/agreement")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(notNullValue());

            verify(service).getForNegotiation(eq("cn1"));
            verify(transformerRegistry).transform(any(ContractAgreement.class), eq(JsonObject.class));
            verifyNoMoreInteractions(transformerRegistry, service);
        }

        @Test
        void getAgreement_transformationFails() {
            when(service.getForNegotiation(eq("cn1"))).thenReturn(createContractAgreement("cn1"));
            when(transformerRegistry.transform(any(ContractAgreement.class), eq(JsonObject.class)))
                    .thenReturn(Result.failure("test-failure"));

            baseRequest(participantContextId)
                    .get("/contractnegotiations/cn1/agreement")
                    .then()
                    .statusCode(500);

            verify(service).getForNegotiation(eq("cn1"));
            verify(transformerRegistry).transform(any(ContractAgreement.class), eq(JsonObject.class));
            verifyNoMoreInteractions(transformerRegistry, service);
        }

        @Test
        void getAgreement_whenNoneFound() {
            when(service.getForNegotiation(eq("cn1"))).thenReturn(null);

            baseRequest(participantContextId)
                    .get("/contractnegotiations/cn1/agreement")
                    .then()
                    .statusCode(404)
                    .contentType(JSON)
                    .body(notNullValue());

            verify(service).getForNegotiation(eq("cn1"));
            verifyNoMoreInteractions(transformerRegistry, service);
        }

    }

    @Nested
    class FindById {

        @Test
        void getById() {
            when(service.findbyId(anyString())).thenReturn(createContractNegotiation("cn1"));
            when(transformerRegistry.transform(any(ContractNegotiation.class), eq(JsonObject.class)))
                    .thenReturn(Result.success(createObjectBuilder().build()));

            baseRequest(participantContextId)
                    .get("/contractnegotiations/cn1")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(notNullValue());

            verify(service).findbyId(anyString());
            verify(transformerRegistry).transform(any(ContractNegotiation.class), eq(JsonObject.class));
        }

        @Test
        void getById_notFound() {
            when(service.findbyId(anyString())).thenReturn(null);

            baseRequest(participantContextId)
                    .get("/contractnegotiations/cn1")
                    .then()
                    .statusCode(404)
                    .contentType(JSON)
                    .body(notNullValue());

            verify(service).findbyId(anyString());
            verifyNoInteractions(transformerRegistry);
        }

        @Test
        void getById_transformationFails() {
            when(service.findbyId(eq("cn1"))).thenReturn(createContractNegotiation("cn1"));
            when(transformerRegistry.transform(any(ContractNegotiation.class), eq(JsonObject.class)))
                    .thenReturn(Result.failure("test-failure"));

            baseRequest(participantContextId)
                    .get("/contractnegotiations/cn1")
                    .then()
                    .statusCode(404)
                    .contentType(JSON)
                    .body(notNullValue());

            verify(service).findbyId(anyString());
            verify(transformerRegistry).transform(any(ContractNegotiation.class), eq(JsonObject.class));
        }

        @Test
        void getById_onlyState() {
            var compacted = createObjectBuilder()
                    .add(VOCAB, EDC_NAMESPACE)
                    .add(TYPE, NEGOTIATION_STATE_TYPE)
                    .add("state", "REQUESTED")
                    .build();

            when(service.getState(eq("cn1"))).thenReturn("REQUESTED");
            when(transformerRegistry.transform(any(NegotiationState.class), eq(JsonObject.class)))
                    .thenReturn(Result.success(compacted));

            baseRequest(participantContextId)
                    .get("/contractnegotiations/cn1/state")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("state", is("REQUESTED"));
            verify(service).getState(eq("cn1"));
            verify(transformerRegistry).transform(any(NegotiationState.class), eq(JsonObject.class));
            verifyNoMoreInteractions(service, transformerRegistry);
        }
    }

    @Nested
    class Query {

        @Test
        void query() {
            when(service.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of(
                    createContractNegotiation("cn1"),
                    createContractNegotiation("cn2")
            )));
            var responseBody = createObjectBuilder().add(ID, "cn").build();

            when(transformerRegistry.transform(any(ContractNegotiation.class), eq(JsonObject.class)))
                    .thenReturn(Result.success(responseBody));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/contractnegotiations/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(2));

            verify(service).search(any(QuerySpec.class));
            verify(transformerRegistry, times(2)).transform(any(ContractNegotiation.class), eq(JsonObject.class));
        }


        @Test
        void query_queryTransformationFails() {
            when(service.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of(
                    createContractNegotiation("cn1"),
                    createContractNegotiation("cn2")
            )));
            when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.failure("test-failure"));

            var requestBody = createObjectBuilder().build();
            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/contractnegotiations/request")
                    .then()
                    .statusCode(400);

            verify(transformerRegistry).transform(any(JsonObject.class), eq(QuerySpec.class));
            verifyNoInteractions(service);
            verifyNoMoreInteractions(transformerRegistry);
        }

        @Test
        void query_dtoTransformationFails() {
            when(service.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of(
                    createContractNegotiation("cn1"),
                    createContractNegotiation("cn2")
            )));
            when(transformerRegistry.transform(any(ContractNegotiation.class), any()))
                    .thenReturn(Result.failure("test-failure"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/contractnegotiations/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(0));

            verify(service).search(any(QuerySpec.class));
        }

        @Test
        void query_singleFailure_shouldLogError() {
            when(service.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of(
                    createContractNegotiation("cn1"),
                    createContractNegotiation("cn2")
            )));
            when(transformerRegistry.transform(any(ContractNegotiation.class), eq(JsonObject.class)))
                    .thenReturn(Result.success(createObjectBuilder().build()))
                    .thenReturn(Result.failure("test-failure"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/contractnegotiations/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(1));

            verify(service).search(any(QuerySpec.class));
            verify(transformerRegistry, times(2)).transform(any(ContractNegotiation.class), eq(JsonObject.class));
            verify(monitor).warning(contains("test-failure"));
        }

        @Test
        void query_jsonObjectTransformationFails() {
            when(service.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of(
                    createContractNegotiation("cn1"),
                    createContractNegotiation("cn2")
            )));
            when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.none()));
            when(transformerRegistry.transform(any(ContractNegotiation.class), eq(JsonObject.class)))
                    .thenReturn(Result.failure("test-failure"));

            var requestBody = createObjectBuilder().build();
            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/contractnegotiations/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(0));

            verify(service).search(any(QuerySpec.class));
            verify(transformerRegistry).transform(any(JsonObject.class), eq(QuerySpec.class));
            verify(transformerRegistry, times(2)).transform(any(ContractNegotiation.class), eq(JsonObject.class));
        }
    }
}
