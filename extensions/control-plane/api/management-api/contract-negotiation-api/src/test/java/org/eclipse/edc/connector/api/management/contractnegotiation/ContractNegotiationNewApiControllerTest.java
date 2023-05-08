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
 *       Microsoft Corporation - Added initiate-negotiation endpoint tests
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.connector.api.management.contractnegotiation;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestData;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.containsString;
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

class ContractNegotiationNewApiControllerTest extends RestControllerTestBase {
    private final ContractNegotiationService service = mock(ContractNegotiationService.class);
    private final TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);
    private final JsonLd jsonLd = new TitaniumJsonLd(monitor);

    @Test
    void getAllContractNegotiations() {

        when(service.query(any(QuerySpec.class))).thenReturn(ServiceResult.success(Stream.of(
                createContractNegotiation("cn1"),
                createContractNegotiation("cn2")
        )));
        when(transformerRegistry.transform(any(QuerySpecDto.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.none()));
        when(transformerRegistry.transform(any(ContractNegotiation.class), eq(ContractNegotiationDto.class)))
                .thenReturn(Result.success(createContractNegotiationDto().build()));
        when(transformerRegistry.transform(any(ContractNegotiationDto.class), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().build()));

        baseRequest()
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(2));

        verify(service).query(any(QuerySpec.class));
        verify(transformerRegistry).transform(any(QuerySpecDto.class), eq(QuerySpec.class));
        verify(transformerRegistry, times(2)).transform(any(ContractNegotiation.class), eq(ContractNegotiationDto.class));
        verify(transformerRegistry, times(2)).transform(any(ContractNegotiationDto.class), eq(JsonObject.class));
    }

    @Test
    void getAllContractNegotiations_queryTransformationFails() {

        when(service.query(any(QuerySpec.class))).thenReturn(ServiceResult.success(Stream.of(
                createContractNegotiation("cn1"),
                createContractNegotiation("cn2")
        )));
        when(transformerRegistry.transform(any(QuerySpecDto.class), eq(QuerySpec.class))).thenReturn(Result.failure("test-failure"));

        baseRequest()
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(400);

        verify(transformerRegistry).transform(any(QuerySpecDto.class), eq(QuerySpec.class));
        verifyNoInteractions(service);
        verifyNoMoreInteractions(transformerRegistry);
    }

    @Test
    void getAllContractNegotiations_dtoTransformationFails() {

        when(service.query(any(QuerySpec.class))).thenReturn(ServiceResult.success(Stream.of(
                createContractNegotiation("cn1"),
                createContractNegotiation("cn2")
        )));
        when(transformerRegistry.transform(any(QuerySpecDto.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.none()));
        when(transformerRegistry.transform(any(ContractNegotiation.class), eq(ContractNegotiationDto.class)))
                .thenReturn(Result.failure("test-failure"));


        baseRequest()
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(0));


        verify(transformerRegistry).transform(any(QuerySpecDto.class), eq(QuerySpec.class));
        verify(transformerRegistry, times(2)).transform(any(ContractNegotiation.class), eq(ContractNegotiationDto.class));
        verify(service).query(any(QuerySpec.class));
        verifyNoMoreInteractions(transformerRegistry);
    }

    @Test
    void getAllContractNegotiations_singleFailure_shouldLogError() {

        when(service.query(any(QuerySpec.class))).thenReturn(ServiceResult.success(Stream.of(
                createContractNegotiation("cn1"),
                createContractNegotiation("cn2")
        )));
        when(transformerRegistry.transform(any(QuerySpecDto.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.none()));
        when(transformerRegistry.transform(any(ContractNegotiation.class), eq(ContractNegotiationDto.class)))
                .thenReturn(Result.success(createContractNegotiationDto().build()));
        when(transformerRegistry.transform(any(ContractNegotiationDto.class), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().build()))
                .thenReturn(Result.failure("test-failure"));

        baseRequest()
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));

        verify(service).query(any(QuerySpec.class));
        verify(transformerRegistry).transform(any(QuerySpecDto.class), eq(QuerySpec.class));
        verify(transformerRegistry, times(2)).transform(any(ContractNegotiation.class), eq(ContractNegotiationDto.class));
        verify(transformerRegistry, times(2)).transform(any(ContractNegotiationDto.class), eq(JsonObject.class));
        verify(monitor).warning(contains("test-failure"));
    }

    @Test
    void getAllContractNegotiations_jsonObjectTransformationFails() {

        when(service.query(any(QuerySpec.class))).thenReturn(ServiceResult.success(Stream.of(
                createContractNegotiation("cn1"),
                createContractNegotiation("cn2")
        )));
        when(transformerRegistry.transform(any(QuerySpecDto.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.none()));
        when(transformerRegistry.transform(any(ContractNegotiation.class), eq(ContractNegotiationDto.class)))
                .thenReturn(Result.success(createContractNegotiationDto().build()));
        when(transformerRegistry.transform(any(ContractNegotiationDto.class), eq(JsonObject.class)))
                .thenReturn(Result.failure("test-failure"));

        baseRequest()
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(0));


        verify(service).query(any(QuerySpec.class));
        verify(transformerRegistry).transform(any(QuerySpecDto.class), eq(QuerySpec.class));
        verify(transformerRegistry, times(2)).transform(any(ContractNegotiation.class), eq(ContractNegotiationDto.class));
        verify(transformerRegistry, times(2)).transform(any(ContractNegotiationDto.class), eq(JsonObject.class));
    }

    @Test
    void getById() {
        when(service.findbyId(anyString())).thenReturn(createContractNegotiation("cn1"));
        when(transformerRegistry.transform(any(ContractNegotiation.class), eq(ContractNegotiationDto.class)))
                .thenReturn(Result.success(createContractNegotiationDto().build()));
        when(transformerRegistry.transform(any(ContractNegotiationDto.class), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().build()));

        baseRequest()
                .get("/cn1")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(notNullValue());

        verify(service).findbyId(anyString());
        verify(transformerRegistry).transform(any(ContractNegotiation.class), eq(ContractNegotiationDto.class));
        verify(transformerRegistry).transform(any(ContractNegotiationDto.class), eq(JsonObject.class));
    }

    @Test
    void getById_notFound() {
        when(service.findbyId(anyString())).thenReturn(null);

        baseRequest()
                .get("/cn1")
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
        when(transformerRegistry.transform(any(ContractNegotiation.class), eq(ContractNegotiationDto.class)))
                .thenReturn(Result.success(createContractNegotiationDto().build()));
        when(transformerRegistry.transform(any(ContractNegotiationDto.class), eq(JsonObject.class)))
                .thenReturn(Result.failure("test-failure"));

        baseRequest()
                .get("/cn1")
                .then()
                .statusCode(400)
                .contentType(JSON)
                .body(notNullValue());

        verify(service).findbyId(anyString());
        verify(transformerRegistry).transform(any(ContractNegotiation.class), eq(ContractNegotiationDto.class));
        verify(transformerRegistry).transform(any(ContractNegotiationDto.class), eq(JsonObject.class));
    }


    @Test
    void getSingleContractNegotationState() {
        when(service.getState(eq("cn1"))).thenReturn("REQUESTED");
        baseRequest()
                .get("/cn1/state")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(Matchers.containsString("REQUESTED"));
        verify(service).getState(eq("cn1"));
        verifyNoMoreInteractions(service, transformerRegistry);
    }

    @Test
    void getSingleContractNegotationAgreement() {
        when(service.getForNegotiation(eq("cn1"))).thenReturn(createContractAgreement("cn1"));
        when(transformerRegistry.transform(any(ContractAgreement.class), eq(ContractAgreementDto.class)))
                .thenReturn(Result.success(ContractAgreementDto.Builder.newInstance().build()));
        when(transformerRegistry.transform(any(ContractAgreementDto.class), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().build()));

        baseRequest()
                .get("/cn1/agreement")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(notNullValue());

        verify(service).getForNegotiation(eq("cn1"));
        verify(transformerRegistry).transform(any(ContractAgreement.class), eq(ContractAgreementDto.class));
        verify(transformerRegistry).transform(any(ContractAgreementDto.class), eq(JsonObject.class));
        verifyNoMoreInteractions(transformerRegistry, service);
    }

    @Test
    void getSingleContractNegotationAgreement_transformationFails() {

        when(service.getForNegotiation(eq("cn1"))).thenReturn(createContractAgreement("cn1"));
        when(transformerRegistry.transform(any(ContractAgreement.class), eq(ContractAgreementDto.class)))
                .thenReturn(Result.failure("test-failure"));

        baseRequest()
                .get("/cn1/agreement")
                .then()
                .statusCode(400);

        verify(service).getForNegotiation(eq("cn1"));
        verify(transformerRegistry).transform(any(ContractAgreement.class), eq(ContractAgreementDto.class));
        verifyNoMoreInteractions(transformerRegistry, service);
    }

    @Test
    void getSingleContractNegotationAgreement_whenNoneFound() {
        when(service.getForNegotiation(eq("cn1"))).thenReturn(null);

        baseRequest()
                .get("/cn1/agreement")
                .then()
                .statusCode(404)
                .contentType(JSON)
                .body(notNullValue());

        verify(service).getForNegotiation(eq("cn1"));
        verifyNoMoreInteractions(transformerRegistry, service);
    }

    @Test
    void initiate() {
        when(transformerRegistry.transform(any(JsonObject.class), eq(NegotiationInitiateRequestDto.class))).thenReturn(Result.success(NegotiationInitiateRequestDto.Builder.newInstance().build()));
        when(transformerRegistry.transform(any(NegotiationInitiateRequestDto.class), eq(ContractRequest.class))).thenReturn(Result.success(
                ContractRequest.Builder.newInstance()
                        .requestData(ContractRequestData.Builder.newInstance()
                                .protocol("test-protocol")
                                .callbackAddress("test-cb")
                                .contractOffer(ContractOffer.Builder.newInstance()
                                        .id("test-offer-id")
                                        .assetId(randomUUID().toString())
                                        .policy(Policy.Builder.newInstance().build())
                                        .build())
                                .build())
                        .build()));
        when(service.initiateNegotiation(any(ContractRequest.class))).thenReturn(createContractNegotiation("cn1"));

        baseRequest()
                .contentType(JSON)
                .body(Json.createObjectBuilder().build())
                .post()
                .then()
                .statusCode(200)
                .body(containsString("\"id\":\"cn1\""));

        verify(service).initiateNegotiation(any());
        verify(transformerRegistry).transform(any(JsonObject.class), eq(NegotiationInitiateRequestDto.class));
        verify(transformerRegistry).transform(any(NegotiationInitiateRequestDto.class), eq(ContractRequest.class));
        verifyNoMoreInteractions(transformerRegistry, service);
    }

    @Test
    void initiate_invalidRequest() {
        when(transformerRegistry.transform(any(JsonObject.class), eq(NegotiationInitiateRequestDto.class))).thenReturn(Result.failure("test-failure"));

        baseRequest()
                .contentType(JSON)
                .body(Json.createObjectBuilder().build())
                .post()
                .then()
                .statusCode(400);
        verify(transformerRegistry).transform(any(JsonObject.class), eq(NegotiationInitiateRequestDto.class));
        verifyNoMoreInteractions(transformerRegistry, service);
    }


    @Test
    void cancel() {
        when(service.cancel(eq("cn1"))).thenReturn(ServiceResult.success(createContractNegotiation("cn1")));
        baseRequest()
                .contentType(JSON)
                .post("/cn1/cancel")
                .then()
                .statusCode(204);
    }

    @Test
    void cancel_failed() {
        when(service.cancel(eq("cn1"))).thenReturn(ServiceResult.badRequest("test-failure"));
        baseRequest()
                .contentType(JSON)
                .post("/cn1/cancel")
                .then()
                .statusCode(400);
    }

    @Test
    void cancel_notFound() {
        when(service.cancel(eq("cn1"))).thenReturn(ServiceResult.notFound("test-failure"));
        baseRequest()
                .contentType(JSON)
                .post("/cn1/cancel")
                .then()
                .statusCode(404);
    }

    @Test
    void cancel_conflict() {
        when(service.cancel(eq("cn1"))).thenReturn(ServiceResult.conflict("test-failure"));
        baseRequest()
                .contentType(JSON)
                .post("/cn1/cancel")
                .then()
                .statusCode(409);
    }

    @Test
    void decline() {
        when(service.decline(eq("cn1"))).thenReturn(ServiceResult.success(createContractNegotiation("cn1")));
        baseRequest()
                .contentType(JSON)
                .post("/cn1/decline")
                .then()
                .statusCode(204);
    }

    @Test
    void decline_failed() {
        when(service.decline(eq("cn1"))).thenReturn(ServiceResult.badRequest("test-failure"));
        baseRequest()
                .contentType(JSON)
                .post("/cn1/decline")
                .then()
                .statusCode(400);
    }

    @Test
    void decline_notFound() {
        when(service.decline(eq("cn1"))).thenReturn(ServiceResult.notFound("test-failure"));
        baseRequest()
                .contentType(JSON)
                .post("/cn1/decline")
                .then()
                .statusCode(404);
    }

    @Test
    void decline_conflict() {
        when(service.decline(eq("cn1"))).thenReturn(ServiceResult.conflict("test-failure"));
        baseRequest()
                .contentType(JSON)
                .post("/cn1/decline")
                .then()
                .statusCode(409);
    }

    @Override
    protected Object controller() {
        return new ContractNegotiationNewApiController(service, transformerRegistry, jsonLd, monitor);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/v2/contractnegotiations")
                .when();
    }

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

    private ContractNegotiationDto.Builder createContractNegotiationDto() {
        return ContractNegotiationDto.Builder.newInstance()
                .id("test-id")
                .contractAgreementId("agreement-id")
                .counterPartyAddress("test-counterparty")
                .type(ContractNegotiation.Type.PROVIDER)
                .protocol("test-protocol")
                .callbackAddresses(List.of(new CallbackAddress()))
                .state(ContractNegotiationStates.INITIAL.toString());

    }
}
