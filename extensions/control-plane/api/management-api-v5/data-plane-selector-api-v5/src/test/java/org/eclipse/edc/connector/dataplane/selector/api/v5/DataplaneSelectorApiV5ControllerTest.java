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

package org.eclipse.edc.connector.dataplane.selector.api.v5;

import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.connector.controlplane.dataplane.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance.DATAPLANE_INSTANCE_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class DataplaneSelectorApiV5ControllerTest extends RestControllerTestBase {

    private final DataPlaneSelectorService selectionService = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final AuthorizationService authorizationService = mock();
    private final String participantContextId = "test-participant-context-id";

    @BeforeEach
    void setup() {
        when(authorizationService.authorize(any(), eq(participantContextId), any(), any())).thenReturn(ServiceResult.success());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{\"@type\":\"QuerySpec\"}"})
    void query_shouldReturnInstancesOfParticipantContext(String body) {
        var instance = createInstance();
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
        when(selectionService.search(any())).thenReturn(ServiceResult.success(List.of(instance)));
        when(transformerRegistry.transform(any(DataPlaneInstance.class), eq(JsonObject.class))).thenReturn(Result.success(createJsonObject(instance)));

        baseRequest()
                .contentType(JSON)
                .body(body)
                .post("/request")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].@id", is(instance.getId()));

        verify(selectionService).search(argThat(query -> query.getFilterExpression().size() == 1 &&
                query.getFilterExpression().contains(new Criterion("participantContextId", "=", participantContextId))));
        verify(authorizationService).authorize(any(), eq(participantContextId), eq(participantContextId), any());
    }

    @Test
    void query_shouldReturnEmptyArray_whenNoInstances() {
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
        when(selectionService.search(any())).thenReturn(ServiceResult.success(List.of()));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, EDC_QUERY_SPEC_TYPE_TERM).build())
                .post("/request")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void query_shouldSkipInstance_whenTransformationFails() {
        var instance = createInstance();
        var failing = createInstance();
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
        when(selectionService.search(any())).thenReturn(ServiceResult.success(List.of(instance, failing)));
        when(transformerRegistry.transform(eq(instance), eq(JsonObject.class))).thenReturn(Result.success(createJsonObject(instance)));
        when(transformerRegistry.transform(eq(failing), eq(JsonObject.class))).thenReturn(Result.failure("failure"));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, EDC_QUERY_SPEC_TYPE_TERM).build())
                .post("/request")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].@id", is(instance.getId()));
    }

    @Test
    void query_shouldReturnForbidden_whenAuthorizationFails() {
        when(authorizationService.authorize(any(), eq(participantContextId), any(), any())).thenReturn(ServiceResult.unauthorized("unauthorized"));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, EDC_QUERY_SPEC_TYPE_TERM).build())
                .post("/request")
                .then()
                .statusCode(403);

        verifyNoInteractions(selectionService, transformerRegistry);
    }

    @Test
    void query_shouldReturnBadRequest_whenQueryTransformationFails() {
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.failure("test-failure"));

        baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, EDC_QUERY_SPEC_TYPE_TERM).build())
                .post("/request")
                .then()
                .statusCode(400);

        verify(transformerRegistry).transform(any(JsonObject.class), eq(QuerySpec.class));
        verifyNoInteractions(selectionService);
    }

    @Test
    void query_shouldReturnBadRequest_whenServiceFails() {
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
        when(selectionService.search(any())).thenReturn(ServiceResult.badRequest("test-message"));

        var error = baseRequest()
                .contentType(JSON)
                .body(createObjectBuilder().add(TYPE, EDC_QUERY_SPEC_TYPE_TERM).build())
                .post("/request")
                .then()
                .statusCode(400)
                .extract().body().as(new TypeRef<List<ApiErrorDetail>>() {
                })
                .get(0);

        assertThat(error.getMessage()).contains("test-message");

        verify(transformerRegistry).transform(any(JsonObject.class), eq(QuerySpec.class));
        verify(selectionService).search(isA(QuerySpec.class));
        verifyNoMoreInteractions(transformerRegistry);
    }

    @Test
    void query_shouldRequireReadScope() throws NoSuchMethodException {
        var method = DataplaneSelectorApiV5Controller.class.getMethod("queryDataPlaneInstancesV5", String.class, JsonObject.class, jakarta.ws.rs.core.SecurityContext.class);

        assertThat(method.getAnnotation(RequiredScope.class)).isNotNull()
                .extracting(RequiredScope::value).isEqualTo("management-api:dataplanes:read");
    }

    @Override
    protected Object controller() {
        return new DataplaneSelectorApiV5Controller(selectionService, transformerRegistry, monitor, authorizationService);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/v5/participants/" + participantContextId + "/dataplanes")
                .when();
    }

    private DataPlaneInstance createInstance() {
        return DataPlaneInstance.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .url("http://somewhere.com:1234/api/v1")
                .participantContextId(participantContextId)
                .build();
    }

    private JsonObject createJsonObject(DataPlaneInstance instance) {
        return createObjectBuilder()
                .add(ID, instance.getId())
                .add(TYPE, DATAPLANE_INSTANCE_TYPE)
                .add(DataPlaneInstance.URL, instance.getUrl().toString())
                .build();
    }
}
