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

package org.eclipse.edc.connector.controlplane.api.management.contractdefinition;

import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public abstract class ContractDefinitionApiControllerTest extends RestControllerTestBase {

    protected final ContractDefinitionService service = mock();
    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final AuthorizationService authService = mock();
    protected final String participantContextId = "test-participant-context-id";

    @BeforeEach
    void setup() {
        when(authService.authorize(any(), eq(participantContextId), any(), any())).thenReturn(ServiceResult.success());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}"})
    void queryAllContractDefinitions(String body) {
        when(service.search(any())).thenReturn(ServiceResult.success(List.of(createContractDefinition().build())));
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
        when(transformerRegistry.transform(any(ContractDefinition.class), eq(JsonObject.class))).thenReturn(Result.success(createExpandedJsonObject()));

        baseRequest(participantContextId)
                .contentType(JSON)
                .body(body)
                .post("/request")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0));

        verify(service).search(argThat(q -> q.getFilterExpression().size() == 1));
    }

    @Test
    void query_authFailure() {
        when(authService.authorize(any(), eq(participantContextId), any(), any())).thenReturn(ServiceResult.unauthorized("unauthorized"));
        baseRequest(participantContextId)
                .contentType(JSON)
                .body("{}")
                .post("/request")
                .then()
                .statusCode(403);
        verifyNoInteractions(service, transformerRegistry);
    }

    @Test
    void queryAll_queryTransformationFails() {
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.failure("test-failure"));
        when(service.search(any())).thenReturn(ServiceResult.success(List.of(createContractDefinition().build())));

        baseRequest(participantContextId)
                .contentType(JSON)
                .body("{}")
                .post("/request")
                .then()
                .statusCode(400);

        verify(transformerRegistry).transform(any(JsonObject.class), eq(QuerySpec.class));
        verifyNoInteractions(service);
    }

    @Test
    void queryAll_serviceBadRequest() {
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
        when(service.search(any())).thenReturn(ServiceResult.badRequest("test-message"));

        var error = baseRequest(participantContextId)
                .contentType(JSON)
                .body("{}")
                .post("/request")
                .then()
                .statusCode(400)
                .extract().body().as(new TypeRef<List<ApiErrorDetail>>() {
                })
                .get(0);

        assertThat(error.getMessage()).contains("test-message");

        verify(transformerRegistry).transform(any(JsonObject.class), eq(QuerySpec.class));
        verify(service).search(isA(QuerySpec.class));
        verifyNoMoreInteractions(transformerRegistry);
    }

    @Test
    void getContractDefById_exists() {
        var entity = createContractDefinition().id("test-id").build();

        when(service.findById(any())).thenReturn(entity);
        when(transformerRegistry.transform(any(ContractDefinition.class), eq(JsonObject.class))).thenReturn(Result.success(createExpandedJsonObject()));
        baseRequest(participantContextId)
                .get("/test-id")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0));

        verify(service).findById(eq(entity.getId()));
    }

    @Test
    void getContractDefById_notExists() {
        when(service.findById(any())).thenReturn(null);

        baseRequest(participantContextId)
                .get("/test-id")
                .then()
                .statusCode(404);

        verify(service).findById("test-id");
        verify(transformerRegistry, never()).transform(any(ContractDefinition.class), eq(JsonObject.class));
    }

    @Test
    void getById_authFailure() {
        when(authService.authorize(any(), eq(participantContextId), any(), any())).thenReturn(ServiceResult.unauthorized("unauthorized"));
        baseRequest(participantContextId)
                .get("/test-id")
                .then()
                .statusCode(403);
        verifyNoInteractions(service, transformerRegistry);
    }

    @Test
    void create() {
        var entity = createContractDefinition().build();
        var requestJson = createExpandedJsonObject();
        var responseBody = createObjectBuilder().add(TYPE, IdResponse.ID_RESPONSE_TYPE).add(ID, entity.getId()).build();
        when(transformerRegistry.transform(any(JsonObject.class), eq(ContractDefinition.class))).thenReturn(Result.success(entity));
        when(service.create(any(ContractDefinition.class))).thenReturn(ServiceResult.success(entity));
        when(transformerRegistry.transform(any(IdResponse.class), eq(JsonObject.class))).thenReturn(Result.success(responseBody));

        baseRequest(participantContextId)
                .contentType(JSON)
                .body(requestJson)
                .post()
                .then()
                .statusCode(200)
                .body(ID, Matchers.equalTo(entity.getId()));

        verify(service).create(any(ContractDefinition.class));
    }

    @Test
    void create_exists() {
        var entity = createContractDefinition().build();
        var requestJson = createExpandedJsonObject();
        when(transformerRegistry.transform(any(JsonObject.class), eq(ContractDefinition.class))).thenReturn(Result.success(entity));
        when(service.create(any(ContractDefinition.class))).thenReturn(ServiceResult.conflict("test-message"));

        baseRequest(participantContextId)
                .contentType(JSON)
                .body(requestJson)
                .post()
                .then()
                .statusCode(409);

        verify(service).create(any(ContractDefinition.class));
    }

    @Test
    void create_transformationFails() {
        var requestJson = createObjectBuilder()
                .add(TYPE, CONTRACT_DEFINITION_TYPE)
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, "ap1")
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, "cp1")
                .add(CONTRACT_DEFINITION_ASSETS_SELECTOR, createCriterionBuilder().build())
                .build();
        when(transformerRegistry.transform(any(JsonObject.class), eq(ContractDefinition.class))).thenReturn(Result.failure("test-failure"));

        baseRequest(participantContextId)
                .contentType(JSON)
                .body(requestJson)
                .post()
                .then()
                .statusCode(400);

        verify(service, never()).create(any(ContractDefinition.class));
    }

    @Test
    void create_authFailure() {
        when(authService.authorize(any(), eq(participantContextId), any(), any())).thenReturn(ServiceResult.unauthorized("unauthorized"));
        baseRequest(participantContextId)
                .contentType(JSON)
                .body("{}")
                .post()
                .then()
                .statusCode(403);
        verifyNoInteractions(service, transformerRegistry);
    }

    @Test
    void delete_exists() {
        var contractDefinition = createContractDefinition().build();
        when(service.delete(eq(contractDefinition.getId()))).thenReturn(ServiceResult.success(contractDefinition));

        baseRequest(participantContextId)
                .delete(contractDefinition.getId())
                .then()
                .statusCode(204);

        verify(service).delete(contractDefinition.getId());
    }

    @Test
    void delete_notExists() {
        var contractDefinition = createContractDefinition().build();
        when(service.delete(eq(contractDefinition.getId()))).thenReturn(ServiceResult.notFound("test-message"));

        baseRequest(participantContextId)
                .delete(contractDefinition.getId())
                .then()
                .statusCode(404);

        verify(service).delete(contractDefinition.getId());
    }

    @Test
    void delete_notPossible() {
        var contractDefinition = createContractDefinition().build();
        when(service.delete(eq(contractDefinition.getId()))).thenReturn(ServiceResult.conflict("test-message"));

        baseRequest(participantContextId)
                .delete(contractDefinition.getId())
                .then()
                .statusCode(409);

        verify(service).delete(contractDefinition.getId());
    }

    @Test
    void delete_authFailure() {
        when(authService.authorize(any(), eq(participantContextId), any(), any())).thenReturn(ServiceResult.unauthorized("unauthorized"));
        baseRequest(participantContextId)
                .delete("some-id")
                .then()
                .statusCode(403);

        verifyNoInteractions(service, transformerRegistry);
    }

    @Test
    void update_whenExists() {
        var entity = createContractDefinition().build();
        var requestJson = createExpandedJsonObject();
        when(transformerRegistry.transform(any(JsonObject.class), eq(ContractDefinition.class))).thenReturn(Result.success(entity));
        when(service.update(any(ContractDefinition.class))).thenReturn(ServiceResult.success());

        baseRequest(participantContextId)
                .contentType(JSON)
                .body(requestJson)
                .put()
                .then()
                .statusCode(204);

        verify(service).update(eq(entity));
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        var entity = createContractDefinition().build();
        var requestJson = createExpandedJsonObject();
        when(transformerRegistry.transform(any(JsonObject.class), eq(ContractDefinition.class))).thenReturn(Result.success(entity));
        when(service.update(any(ContractDefinition.class))).thenReturn(ServiceResult.notFound("test-message"));

        baseRequest(participantContextId)
                .contentType(JSON)
                .body(requestJson)
                .put()
                .then()
                .statusCode(404);

        verify(service).update(eq(entity));
    }

    @Test
    void update_whenTransformationFails_shouldThrowException() {
        var entity = createContractDefinition().build();
        when(transformerRegistry.transform(any(JsonObject.class), eq(ContractDefinition.class))).thenReturn(Result.failure("test-failure"));
        when(service.update(any(ContractDefinition.class))).thenReturn(ServiceResult.success());

        var requestJson = createExpandedJsonObject();

        baseRequest(participantContextId)
                .contentType(JSON)
                .body(requestJson)
                .put()
                .then()
                .statusCode(400);

        verify(service, never()).update(eq(entity));
    }

    @Test
    void update_authFailure() {
        when(authService.authorize(any(), eq(participantContextId), eq("id"), any())).thenReturn(ServiceResult.unauthorized("unauthorized"));
        var entity = createContractDefinition().id("id").build();
        when(transformerRegistry.transform(any(JsonObject.class), eq(ContractDefinition.class))).thenReturn(Result.success(entity));

        baseRequest(participantContextId)
                .contentType(JSON)
                .body("{}")
                .put()
                .then()
                .statusCode(403);
        verify(transformerRegistry).transform(any(JsonObject.class), eq(ContractDefinition.class));
        verifyNoMoreInteractions(service, transformerRegistry);
    }

    protected abstract RequestSpecification baseRequest(String participantContextId);

    private JsonArrayBuilder createCriterionBuilder() {
        return Json.createArrayBuilder()
                .add(createObjectBuilder()
                        .add(TYPE, EDC_NAMESPACE + "Criterion")
                        .add(EDC_NAMESPACE + "operandLeft", "foo")
                        .add(EDC_NAMESPACE + "operator", "=")
                        .add(EDC_NAMESPACE + "operandRight", "bar")
                );
    }

    private JsonObject createExpandedJsonObject() {
        return createObjectBuilder()
                .add(TYPE, CONTRACT_DEFINITION_TYPE)
                .add(ID, "test-id")
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, "ap1")
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, "cp1")
                .add(CONTRACT_DEFINITION_ASSETS_SELECTOR, createCriterionBuilder().build())
                .build();
    }

    private ContractDefinition.Builder createContractDefinition() {
        return ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId("ap-id")
                .contractPolicyId("cp-id");
    }
}