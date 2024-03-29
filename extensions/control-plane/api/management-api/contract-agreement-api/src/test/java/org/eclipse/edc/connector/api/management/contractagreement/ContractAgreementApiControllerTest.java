/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.contractagreement;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class ContractAgreementApiControllerTest extends RestControllerTestBase {

    private final ContractAgreementService service = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final JsonObjectValidatorRegistry validatorRegistry = mock();

    @Test
    void queryAllAgreements_whenExists() {
        var expanded = Json.createObjectBuilder().build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.none()));
        when(service.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of(createContractAgreement("id1"), createContractAgreement("id2"))));
        when(transformerRegistry.transform(any(ContractAgreement.class), eq(JsonObject.class))).thenReturn(Result.success(expanded));

        baseRequest()
                .contentType(JSON)
                .body("{}")
                .post("/request")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2));

        verify(validatorRegistry).validate(eq(EDC_QUERY_SPEC_TYPE), any());
        verify(transformerRegistry).transform(any(JsonObject.class), eq(QuerySpec.class));
        verify(service).search(any(QuerySpec.class));
        verify(transformerRegistry, times(2)).transform(any(ContractAgreement.class), eq(JsonObject.class));
        verifyNoMoreInteractions(service, transformerRegistry);
    }

    @Test
    void queryAllAgreements_whenNoneExists() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.none()));
        when(service.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(emptyList()));

        baseRequest()
                .contentType(JSON)
                .body("{}")
                .post("/request")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));

        verify(transformerRegistry).transform(any(JsonObject.class), eq(QuerySpec.class));
        verify(service).search(any(QuerySpec.class));
        verify(transformerRegistry, never()).transform(any(ContractAgreement.class), eq(JsonObject.class));
        verifyNoMoreInteractions(service, transformerRegistry);
    }

    @Test
    void queryAllAgreements_shouldReturnBadRequest_whenValidationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(Violation.violation("failure", "failing path")));

        baseRequest()
                .contentType(JSON)
                .body("{}")
                .post("/request")
                .then()
                .statusCode(400);

        verifyNoInteractions(service, transformerRegistry);
    }

    @Test
    void queryAllAgreements_whenTransformationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.none()));
        when(service.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of(createContractAgreement("id1"), createContractAgreement("id2"))));
        when(transformerRegistry.transform(any(ContractAgreement.class), eq(JsonObject.class))).thenReturn(Result.failure("test-failure"));

        baseRequest()
                .contentType(JSON)
                .body("{}")
                .post("/request")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));

        verify(transformerRegistry).transform(any(JsonObject.class), eq(QuerySpec.class));
        verify(service).search(any(QuerySpec.class));
        verify(transformerRegistry, times(2)).transform(any(ContractAgreement.class), eq(JsonObject.class));
        verify(monitor, times(2)).warning(eq("test-failure"));
        verifyNoMoreInteractions(service, transformerRegistry);
    }

    @Test
    void getContractAgreement() {
        when(service.findById(eq("id1"))).thenReturn(createContractAgreement("id1"));
        when(transformerRegistry.transform(any(ContractAgreement.class), eq(JsonObject.class))).thenReturn(Result.success(Json.createObjectBuilder().build()));

        baseRequest()
                .contentType(JSON)
                .get("/id1")
                .then()
                .statusCode(200)
                .body(notNullValue());

        verify(service).findById(eq("id1"));
        verify(transformerRegistry).transform(any(ContractAgreement.class), eq(JsonObject.class));
        verifyNoMoreInteractions(service, transformerRegistry);
    }

    @Test
    void getContractAgreement_notFound() {
        when(service.findById(eq("id1"))).thenReturn(null);

        baseRequest()
                .contentType(JSON)
                .get("/id1")
                .then()
                .statusCode(404)
                .body(notNullValue());

        verify(service).findById(eq("id1"));
        verifyNoMoreInteractions(service, transformerRegistry);
    }

    @Test
    void getContractAgreement_transformationFails() {
        when(service.findById(eq("id1"))).thenReturn(createContractAgreement("id1"));
        when(transformerRegistry.transform(any(ContractAgreement.class), eq(JsonObject.class))).thenReturn(Result.failure("test-failure"));

        baseRequest()
                .contentType(JSON)
                .get("/id1")
                .then()
                .statusCode(500);

        verify(service).findById(eq("id1"));
        verify(transformerRegistry).transform(any(ContractAgreement.class), eq(JsonObject.class));
        verifyNoMoreInteractions(service, transformerRegistry);
    }


    @Test
    void getContractNegotiation() {
        when(service.findNegotiation(any())).thenReturn(createContractNegotiationBuilder("negotiation-id").build());
        when(transformerRegistry.transform(any(ContractNegotiation.class), eq(JsonObject.class))).thenReturn(Result.success(Json.createObjectBuilder().build()));

        baseRequest()
                .contentType(JSON)
                .get("/agreement-id/negotiation")
                .then()
                .statusCode(200)
                .body(notNullValue());

        verify(service).findNegotiation(eq("agreement-id"));
        verify(transformerRegistry).transform(any(ContractNegotiation.class), eq(JsonObject.class));
        verifyNoMoreInteractions(service, transformerRegistry);
    }

    @Test
    void getContractNegotiation_notFound() {
        when(service.findNegotiation(any())).thenReturn(null);

        baseRequest()
                .contentType(JSON)
                .get("/agreement-id/negotiation")
                .then()
                .statusCode(404)
                .body(notNullValue());

        verify(service).findNegotiation("agreement-id");
        verifyNoMoreInteractions(service, transformerRegistry);
    }

    @Test
    void getContractNegotiation_transformationFails() {
        when(service.findNegotiation(any())).thenReturn(createContractNegotiationBuilder("agreement-id").build());
        when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.failure("test-failure"));

        baseRequest()
                .contentType(JSON)
                .get("/agreement-id/negotiation")
                .then()
                .statusCode(500);

        verify(service).findNegotiation("agreement-id");
        verify(transformerRegistry).transform(isA(ContractNegotiation.class), eq(JsonObject.class));
        verifyNoMoreInteractions(service, transformerRegistry);
    }

    @Override
    protected Object controller() {
        return new ContractAgreementApiController(service, transformerRegistry, monitor, validatorRegistry);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/v2/contractagreements")
                .when();
    }

    private ContractAgreement createContractAgreement(String negotiationId) {
        return ContractAgreement.Builder.newInstance()
                .id(negotiationId)
                .consumerId("test-consumer")
                .providerId("test-provider")
                .assetId(UUID.randomUUID().toString())
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

}
