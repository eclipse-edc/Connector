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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.model.NegotiationState;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.eclipse.edc.api.model.IdResponse.ID_RESPONSE_TYPE;
import static org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.model.NegotiationState.NEGOTIATION_STATE_TYPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand.TERMINATE_NEGOTIATION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.validator.spi.Violation.violation;
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

public abstract class BaseContractNegotiationApiControllerTest extends RestControllerTestBase {
    protected final ContractNegotiationService service = mock();
    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final JsonObjectValidatorRegistry validatorRegistry = mock();

    @Test
    void getAll() {
        when(service.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of(
                createContractNegotiation("cn1"),
                createContractNegotiation("cn2")
        )));
        var responseBody = createObjectBuilder().add(ID, "cn").build();

        when(transformerRegistry.transform(any(ContractNegotiation.class), eq(JsonObject.class)))
                .thenReturn(Result.success(responseBody));

        baseRequest()
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(2));

        verifyNoInteractions(validatorRegistry);
        verify(service).search(any(QuerySpec.class));
        verify(transformerRegistry, times(2)).transform(any(ContractNegotiation.class), eq(JsonObject.class));
    }

    @Test
    void getAll_shouldReturnBadRequest_whenValidationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("failure", "path")));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().build())
                .post("/request")
                .then()
                .statusCode(400);

        verify(validatorRegistry).validate(eq(EDC_QUERY_SPEC_TYPE), any());
        verifyNoInteractions(service, transformerRegistry);
    }

    @Test
    void getAll_queryTransformationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(service.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of(
                createContractNegotiation("cn1"),
                createContractNegotiation("cn2")
        )));
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.failure("test-failure"));

        var requestBody = createObjectBuilder().build();
        baseRequest()
                .contentType(JSON)
                .body(requestBody)
                .post("/request")
                .then()
                .statusCode(400);

        verify(transformerRegistry).transform(any(JsonObject.class), eq(QuerySpec.class));
        verifyNoInteractions(service);
        verifyNoMoreInteractions(transformerRegistry);
    }

    @Test
    void getAll_dtoTransformationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(service.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of(
                createContractNegotiation("cn1"),
                createContractNegotiation("cn2")
        )));
        when(transformerRegistry.transform(any(ContractNegotiation.class), any()))
                .thenReturn(Result.failure("test-failure"));

        baseRequest()
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(0));

        verify(service).search(any(QuerySpec.class));
    }

    @Test
    void getAll_singleFailure_shouldLogError() {
        when(service.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of(
                createContractNegotiation("cn1"),
                createContractNegotiation("cn2")
        )));
        when(transformerRegistry.transform(any(ContractNegotiation.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createObjectBuilder().build()))
                .thenReturn(Result.failure("test-failure"));

        baseRequest()
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));

        verify(service).search(any(QuerySpec.class));
        verify(transformerRegistry, times(2)).transform(any(ContractNegotiation.class), eq(JsonObject.class));
        verify(monitor).warning(contains("test-failure"));
    }

    @Test
    void getAll_jsonObjectTransformationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(service.search(any(QuerySpec.class))).thenReturn(ServiceResult.success(List.of(
                createContractNegotiation("cn1"),
                createContractNegotiation("cn2")
        )));
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.none()));
        when(transformerRegistry.transform(any(ContractNegotiation.class), eq(JsonObject.class)))
                .thenReturn(Result.failure("test-failure"));

        var requestBody = createObjectBuilder().build();
        baseRequest()
                .contentType(JSON)
                .body(requestBody)
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(0));

        verify(service).search(any(QuerySpec.class));
        verify(transformerRegistry).transform(any(JsonObject.class), eq(QuerySpec.class));
        verify(transformerRegistry, times(2)).transform(any(ContractNegotiation.class), eq(JsonObject.class));
    }

    @Test
    void getById() {
        when(service.findbyId(anyString())).thenReturn(createContractNegotiation("cn1"));
        when(transformerRegistry.transform(any(ContractNegotiation.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createObjectBuilder().build()));

        baseRequest()
                .get("/cn1")
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
        when(transformerRegistry.transform(any(ContractNegotiation.class), eq(JsonObject.class)))
                .thenReturn(Result.failure("test-failure"));

        baseRequest()
                .get("/cn1")
                .then()
                .statusCode(404)
                .contentType(JSON)
                .body(notNullValue());

        verify(service).findbyId(anyString());
        verify(transformerRegistry).transform(any(ContractNegotiation.class), eq(JsonObject.class));
    }

    @Test
    void getSingleContractNegotiationState() {
        var compacted = createObjectBuilder()
                .add(VOCAB, EDC_NAMESPACE)
                .add(TYPE, NEGOTIATION_STATE_TYPE)
                .add("state", "REQUESTED")
                .build();

        when(service.getState(eq("cn1"))).thenReturn("REQUESTED");
        when(transformerRegistry.transform(any(NegotiationState.class), eq(JsonObject.class)))
                .thenReturn(Result.success(compacted));

        baseRequest()
                .get("/cn1/state")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("state", is("REQUESTED"));
        verify(service).getState(eq("cn1"));
        verify(transformerRegistry).transform(any(NegotiationState.class), eq(JsonObject.class));
        verifyNoMoreInteractions(service, transformerRegistry);
    }

    @Test
    void getSingleContractNegotiationAgreement() {
        when(service.getForNegotiation(eq("cn1"))).thenReturn(createContractAgreement("cn1"));
        when(transformerRegistry.transform(any(ContractAgreement.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createObjectBuilder().build()));

        baseRequest()
                .get("/cn1/agreement")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(notNullValue());

        verify(service).getForNegotiation(eq("cn1"));
        verify(transformerRegistry).transform(any(ContractAgreement.class), eq(JsonObject.class));
        verifyNoMoreInteractions(transformerRegistry, service);
    }

    @Test
    void getSingleContractNegotiationAgreement_transformationFails() {
        when(service.getForNegotiation(eq("cn1"))).thenReturn(createContractAgreement("cn1"));
        when(transformerRegistry.transform(any(ContractAgreement.class), eq(JsonObject.class)))
                .thenReturn(Result.failure("test-failure"));

        baseRequest()
                .get("/cn1/agreement")
                .then()
                .statusCode(500);

        verify(service).getForNegotiation(eq("cn1"));
        verify(transformerRegistry).transform(any(ContractAgreement.class), eq(JsonObject.class));
        verifyNoMoreInteractions(transformerRegistry, service);
    }

    @Test
    void getSingleContractNegotiationAgreement_whenNoneFound() {
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
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
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

        when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(responseBody));
        when(service.initiateNegotiation(any(ContractRequest.class))).thenReturn(contractNegotiation);

        when(transformerRegistry.transform(any(IdResponse.class), eq(JsonObject.class))).thenReturn(Result.success(responseBody));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().build())
                .post()
                .then()
                .statusCode(200)
                .body(ID, is(contractNegotiation.getId()));

        verify(service).initiateNegotiation(any());
        verify(transformerRegistry).transform(any(JsonObject.class), eq(ContractRequest.class));
        verify(transformerRegistry).transform(any(IdResponse.class), eq(JsonObject.class));
        verifyNoMoreInteractions(transformerRegistry, service);
    }

    @Test
    void initiate_shouldReturnBadRequest_whenValidationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("error", "path")));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().build())
                .post()
                .then()
                .statusCode(400);

        verify(validatorRegistry).validate(eq(ContractRequest.CONTRACT_REQUEST_TYPE), any());
        verifyNoInteractions(transformerRegistry, service);
    }

    @Test
    void initiate_invalidRequest() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(JsonObject.class), any())).thenReturn(Result.failure("test-failure"));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().build())
                .post()
                .then()
                .statusCode(400);
        verifyNoMoreInteractions(service);
    }

    @Test
    void terminate_shouldCallService() {
        var command = new TerminateNegotiationCommand("id", "reason");
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(JsonObject.class), eq(TerminateNegotiationCommand.class))).thenReturn(Result.success(command));
        when(service.terminate(any())).thenReturn(ServiceResult.success());

        baseRequest()
                .body(Json.createObjectBuilder().add(ID, "id").build())
                .contentType(JSON)
                .post("/cn1/terminate")
                .then()
                .statusCode(204);

        verify(service).terminate(command);
    }

    @Test
    void terminate_shouldReturnBadRequest_whenValidationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(JsonObject.class), eq(TerminateNegotiationCommand.class))).thenReturn(Result.failure("error"));

        baseRequest()
                .body(Json.createObjectBuilder().add(ID, "id").build())
                .contentType(JSON)
                .post("/cn1/terminate")
                .then()
                .statusCode(400);

        verifyNoInteractions(service);
    }

    @Test
    void terminate_shouldReturnError_whenServiceFails() {
        var command = new TerminateNegotiationCommand("id", "reason");
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(JsonObject.class), eq(TerminateNegotiationCommand.class))).thenReturn(Result.success(command));
        when(service.terminate(any())).thenReturn(ServiceResult.conflict("conflict"));

        baseRequest()
                .body(Json.createObjectBuilder().add(ID, "id").build())
                .contentType(JSON)
                .post("/cn1/terminate")
                .then()
                .statusCode(409);
    }

    @Test
    void terminate_shouldReturnBadRequest_whenTransformationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("error", "path")));

        baseRequest()
                .body(Json.createObjectBuilder().add(ID, "id").build())
                .contentType(JSON)
                .post("/cn1/terminate")
                .then()
                .statusCode(400);

        verify(validatorRegistry).validate(eq(TERMINATE_NEGOTIATION_TYPE), any());
        verifyNoInteractions(transformerRegistry, service);
    }

    @Test
    void delete_shouldCallService() {
        when(service.delete(any())).thenReturn(ServiceResult.success());

        baseRequest()
                .contentType(JSON)
                .delete("/cn1")
                .then()
                .statusCode(204);

        verify(service).delete("cn1");
    }

    @Test
    void delete_shouldFailDueToWrongState() {
        when(service.delete(any())).thenReturn(ServiceResult.conflict(format("Cannot delete negotiation in state: %s".formatted(ContractNegotiationStates.AGREED.name()))));

        baseRequest()
                .contentType(JSON)
                .delete("/cn1")
                .then()
                .statusCode(409);

        verify(service).delete("cn1");
    }

    protected abstract RequestSpecification baseRequest();

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
}
