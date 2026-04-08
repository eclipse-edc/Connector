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

package org.eclipse.edc.connector.controlplane.api.management.asset;

import io.restassured.specification.RequestSpecification;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.api.model.IdResponse.ID_RESPONSE_CREATED_AT;
import static org.eclipse.edc.api.model.IdResponse.ID_RESPONSE_TYPE;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ApiTest
public abstract class AssetApiControllerTest extends RestControllerTestBase {

    private static final String TEST_ASSET_ID = "test-asset-id";
    private static final String TEST_ASSET_CONTENTTYPE = "application/json";
    private static final String TEST_ASSET_DESCRIPTION = "test description";
    private static final String TEST_ASSET_VERSION = "0.4.2";
    private static final String TEST_ASSET_NAME = "test-asset";
    protected final AssetService assetService = mock();
    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final JsonObjectValidatorRegistry validator = mock();
    protected final AuthorizationService authorizationService = mock();
    private final String participantContextId = "test-participant-context-id";

    @BeforeEach
    void setup() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(DataAddress.class))).thenReturn(Result.success(DataAddress.Builder.newInstance().type("test-type").build()));
        when(transformerRegistry.transform(isA(IdResponse.class), eq(JsonObject.class))).thenAnswer(a -> {
            var idResponse = (IdResponse) a.getArgument(0);
            return Result.success(createObjectBuilder()
                    .add(TYPE, ID_RESPONSE_TYPE)
                    .add(ID, idResponse.getId())
                    .add(ID_RESPONSE_CREATED_AT, idResponse.getCreatedAt())
                    .build()
            );
        });
        when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(ServiceResult.success());
    }

    protected abstract String versionPath();

    private JsonObjectBuilder createAssetJson() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, EDC_ASSET_TYPE)
                .add(ID, TEST_ASSET_ID)
                .add("properties", createPropertiesBuilder().build());
    }

    private JsonObjectBuilder createPropertiesBuilder() {
        return createObjectBuilder()
                .add("name", TEST_ASSET_NAME)
                .add("description", TEST_ASSET_DESCRIPTION)
                .add("edc:version", TEST_ASSET_VERSION)
                .add("contenttype", TEST_ASSET_CONTENTTYPE);
    }

    private JsonObjectBuilder createContextBuilder() {
        return createObjectBuilder()
                .add(VOCAB, EDC_NAMESPACE)
                .add(EDC_PREFIX, EDC_NAMESPACE);
    }

    private RequestSpecification baseRequest(String participantContextId) {
        return given()
                .baseUri("http://localhost:" + port + "/" + versionPath() + "/participants/" + participantContextId)
                .when();
    }

    private Asset.Builder createAssetBuilder() {
        return Asset.Builder.newInstance()
                .id(TEST_ASSET_ID)
                .description(TEST_ASSET_DESCRIPTION);
    }

    @Nested
    class FindById {
        @Test
        void findById_success() {
            var asset = Asset.Builder.newInstance().property("key", "value").build();
            when(assetService.findById("id")).thenReturn(asset);
            var assetJson = createAssetJson().build();
            when(transformerRegistry.transform(isA(Asset.class), eq(JsonObject.class))).thenReturn(Result.success(assetJson));

            baseRequest(participantContextId)
                    .get("/assets/id")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, equalTo(TEST_ASSET_ID));

            verify(transformerRegistry).transform(isA(Asset.class), eq(JsonObject.class));
            verifyNoMoreInteractions(transformerRegistry);
        }

        @Test
        void findById_notFound() {
            when(assetService.findById(any())).thenReturn(null);

            baseRequest(participantContextId)
                    .get("/assets/not-existent-id")
                    .then()
                    .statusCode(404);
        }

        @Test
        void findById_authorizationFailed() {

            when(authorizationService.authorize(any(), any(), any(), any()))
                    .thenReturn(ServiceResult.unauthorized("unauthorized"));

            baseRequest(participantContextId)
                    .get("/assets/not-existent-id")
                    .then()
                    .statusCode(403);
        }

        @Test
        void findById_shouldReturnNotFound_whenTransformFails() {
            when(assetService.findById("id")).thenReturn(Asset.Builder.newInstance().build());
            when(transformerRegistry.transform(isA(Asset.class), eq(JsonObject.class))).thenReturn(Result.failure("failure"));

            baseRequest(participantContextId)
                    .get("/assets/id")
                    .then()
                    .statusCode(500);
        }
    }

    @Nested
    class Create {
        @Test
        void createAsset() {
            var asset = createAssetBuilder().dataAddress(DataAddress.Builder.newInstance().type("any").build()).build();
            when(transformerRegistry.transform(any(JsonObject.class), eq(Asset.class))).thenReturn(Result.success(asset));
            when(assetService.create(isA(Asset.class))).thenReturn(ServiceResult.success(asset));
            when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(createAssetJson().build())
                    .post("/assets")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is(TEST_ASSET_ID))
                    .body("'" + EDC_NAMESPACE + "createdAt'", greaterThan(0L));

            verify(transformerRegistry).transform(any(), eq(Asset.class));
            verify(transformerRegistry).transform(isA(IdResponse.class), eq(JsonObject.class));
            verify(assetService).create(isA(Asset.class));
            verifyNoMoreInteractions(assetService, transformerRegistry);
        }

        @Test
        void createAsset_authorizationFailed() {
            when(authorizationService.authorize(any(), any(), any(), any()))
                    .thenReturn(ServiceResult.unauthorized("unauthorized"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(createAssetJson().build())
                    .post("/assets")
                    .then()
                    .statusCode(403);

            verify(authorizationService).authorize(any(), any(), any(), any());
            verifyNoMoreInteractions(assetService, transformerRegistry, authorizationService);
        }

        @Test
        public void createAsset_shouldReturnBadRequest_whenValidationFails() {
            when(validator.validate(any(), any())).thenReturn(ValidationResult.failure(violation("a failure", "a path")));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(createAssetJson().build())
                    .post("/assets")
                    .then()
                    .statusCode(400);

            verify(validator).validate(eq(EDC_ASSET_TYPE), isA(JsonObject.class));
            verifyNoInteractions(assetService, transformerRegistry);
        }

        @Test
        void createAsset_shouldReturnBadRequest_whenTransformFails() {
            when(transformerRegistry.transform(isA(JsonObject.class), eq(Asset.class))).thenReturn(Result.failure("failed"));
            when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

            baseRequest(participantContextId)
                    .body(createAssetJson().build())
                    .contentType(JSON)
                    .post("/assets")
                    .then()
                    .statusCode(400);
            verifyNoInteractions(assetService);
        }

        @Test
        void createAsset_alreadyExists() {
            var asset = createAssetBuilder().dataAddress(DataAddress.Builder.newInstance().type("any").build()).build();
            when(transformerRegistry.transform(any(JsonObject.class), eq(Asset.class))).thenReturn(Result.success(asset));
            when(assetService.create(isA(Asset.class))).thenReturn(ServiceResult.conflict("already exists"));
            when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

            baseRequest(participantContextId)
                    .body(createAssetJson().build())
                    .contentType(JSON)
                    .post("/assets")
                    .then()
                    .statusCode(409);
        }

        @Test
        void createAsset_emptyAttributes() {
            when(transformerRegistry.transform(isA(JsonObject.class), any())).thenReturn(Result.failure("Cannot be transformed"));
            when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

            baseRequest(participantContextId)
                    .body(createAssetJson().build())
                    .contentType(JSON)
                    .post("/assets")
                    .then()
                    .statusCode(400);
        }
    }

    @Nested
    class Delete {
        @Test
        void deleteAsset() {
            when(assetService.delete("assetId"))
                    .thenReturn(ServiceResult.success(createAssetBuilder().build()));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .delete("/assets/assetId")
                    .then()
                    .statusCode(204);
            verify(assetService).delete("assetId");
        }

        @Test
        void deleteAsset_authorizationFailed() {
            when(authorizationService.authorize(any(), any(), any(), any()))
                    .thenReturn(ServiceResult.unauthorized("unauthorized"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .delete("/assets/assetId")
                    .then()
                    .statusCode(403);

            verify(authorizationService).authorize(any(), any(), any(), any());
            verifyNoMoreInteractions(assetService, transformerRegistry, authorizationService);
        }

        @Test
        void deleteAsset_notExists() {
            when(assetService.delete(any())).thenReturn(ServiceResult.notFound("not found"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .delete("/assets/not-existent-id")
                    .then()
                    .statusCode(404);
        }

        @Test
        void deleteAsset_conflicts() {
            when(assetService.delete(any())).thenReturn(ServiceResult.conflict("conflict"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .delete("/assets/id")
                    .then()
                    .statusCode(409);
        }
    }

    @Nested
    class Update {
        @Test
        void updateAsset_whenExists() {
            var asset = Asset.Builder.newInstance().property("key1", "value1").participantContextId(participantContextId).build();
            when(transformerRegistry.transform(isA(JsonObject.class), eq(Asset.class))).thenReturn(Result.success(asset));
            when(assetService.update(any(Asset.class))).thenReturn(ServiceResult.success());
            when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

            baseRequest(participantContextId)
                    .body(createAssetJson().build())
                    .contentType(JSON)
                    .put("/assets")
                    .then()
                    .statusCode(204);
            verify(assetService).update(refEq(asset));
        }

        @Test
        void updateAsset_authorizationFailed() {
            var asset = Asset.Builder.newInstance().property("key1", "value1").build();
            when(transformerRegistry.transform(isA(JsonObject.class), eq(Asset.class))).thenReturn(Result.success(asset));
            when(validator.validate(any(), any())).thenReturn(ValidationResult.success());
            when(authorizationService.authorize(any(), any(), any(), any()))
                    .thenReturn(ServiceResult.unauthorized("unauthorized"));


            baseRequest(participantContextId)
                    .body(createAssetJson().build())
                    .contentType(JSON)
                    .put("/assets")
                    .then()
                    .statusCode(403);
            verify(authorizationService).authorize(any(), any(), any(), any());
            verify(transformerRegistry).transform(any(), eq(Asset.class));
            verify(validator).validate(eq(EDC_ASSET_TYPE), isA(JsonObject.class));
            verifyNoMoreInteractions(assetService, transformerRegistry, authorizationService);
        }

        @Test
        void updateAsset_shouldReturnNotFound_whenItDoesNotExists() {
            var asset = Asset.Builder.newInstance().property("key1", "value1").build();
            when(transformerRegistry.transform(isA(JsonObject.class), eq(Asset.class))).thenReturn(Result.success(asset));
            when(assetService.update(any(Asset.class))).thenReturn(ServiceResult.notFound("not found"));
            when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

            baseRequest(participantContextId)
                    .body(createAssetJson().build())
                    .contentType(JSON)
                    .put("/assets")
                    .then()
                    .statusCode(404);
        }

        @Test
        void updateAsset_shouldReturnBadRequest_whenTransformFails() {
            when(transformerRegistry.transform(isA(JsonObject.class), eq(Asset.class))).thenReturn(Result.failure("error"));
            when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

            baseRequest(participantContextId)
                    .body(createAssetJson().build())
                    .contentType(JSON)
                    .put("/assets")
                    .then()
                    .statusCode(400);
            verifyNoInteractions(assetService);
        }

        @Test
        public void updateAsset_shouldReturnBadRequest_whenValidationFails() {
            when(transformerRegistry.transform(isA(JsonObject.class), eq(Asset.class))).thenReturn(Result.failure("error"));
            when(validator.validate(any(), any())).thenReturn(ValidationResult.failure(violation("validation failure", "path")));

            baseRequest(participantContextId)
                    .body(createAssetJson().build())
                    .contentType(JSON)
                    .put("/assets")
                    .then()
                    .statusCode(400);
            verify(validator).validate(eq(EDC_ASSET_TYPE), isA(JsonObject.class));
            verifyNoInteractions(assetService, transformerRegistry);
        }

    }

    @Nested
    class Query {
        @Test
        void requestAsset() {
            when(assetService.search(any()))
                    .thenReturn(ServiceResult.success(List.of(Asset.Builder.newInstance().build())));
            when(transformerRegistry.transform(isA(Asset.class), eq(JsonObject.class)))
                    .thenReturn(Result.success(createAssetJson().build()));
            when(transformerRegistry.transform(isA(JsonObject.class), eq(QuerySpec.class)))
                    .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
            when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body("{}")
                    .post("/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(1));
            verify(assetService).search(argThat(s -> s.getOffset() == 10 &&
                    s.getFilterExpression().stream().anyMatch(c -> c.getOperandLeft().equals("participantContextId") &&
                            c.getOperator().equals("=") &&
                            c.getOperandRight().equals(participantContextId))));
            verify(transformerRegistry).transform(isA(Asset.class), eq(JsonObject.class));
            verify(transformerRegistry).transform(isA(JsonObject.class), eq(QuerySpec.class));
        }

        @Test
        void requestAsset_authorizationFailed() {
            when(authorizationService.authorize(any(), any(), any(), any()))
                    .thenReturn(ServiceResult.unauthorized("unauthorized"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body("{}")
                    .post("/assets/request")
                    .then()
                    .log().ifError()
                    .statusCode(403);
            verify(authorizationService).authorize(any(), any(), any(), any());
            verifyNoMoreInteractions(authorizationService, assetService, transformerRegistry, validator);
        }

        @Test
        void requestAsset_filtersOutFailedTransforms() {
            when(assetService.search(any()))
                    .thenReturn(ServiceResult.success(List.of(Asset.Builder.newInstance().build())));
            when(transformerRegistry.transform(isA(JsonObject.class), eq(QuerySpec.class)))
                    .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
            when(transformerRegistry.transform(isA(Asset.class), eq(JsonObject.class)))
                    .thenReturn(Result.failure("failed to transform"));
            when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/assets/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(0));
        }

        @Test
        void requestAsset_shouldReturnBadRequest_whenQueryIsInvalid() {
            when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
            when(assetService.search(any())).thenReturn(ServiceResult.badRequest("test-message"));
            when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

            baseRequest(participantContextId)
                    .body(Map.of("offset", -1))
                    .contentType(JSON)
                    .post("/assets/request")
                    .then()
                    .statusCode(400);
        }

        @Test
        void requestAsset_shouldReturnBadRequest_whenQueryTransformFails() {
            when(transformerRegistry.transform(isA(JsonObject.class), eq(QuerySpec.class)))
                    .thenReturn(Result.failure("error"));
            when(assetService.search(any())).thenReturn(ServiceResult.success());
            when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body("{}")
                    .post("/assets/request")
                    .then()
                    .statusCode(400);
        }

        @Test
        void requestAsset_shouldReturnBadRequest_whenServiceReturnsBadRequest() {
            when(transformerRegistry.transform(isA(JsonObject.class), eq(QuerySpec.class)))
                    .thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
            when(assetService.search(any())).thenReturn(ServiceResult.badRequest());
            when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/assets/request")
                    .then()
                    .statusCode(400);
        }

        @Test
        public void requestAsset_shouldReturnBadRequest_whenValidationFails() {
            when(transformerRegistry.transform(isA(JsonObject.class), eq(QuerySpec.class)))
                    .thenReturn(Result.success(QuerySpec.Builder.newInstance().build()));
            when(validator.validate(any(), any())).thenReturn(ValidationResult.failure(violation("validation failure", "a path")));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body("{}")
                    .post("/assets/request")
                    .then()
                    .statusCode(400);
            verify(validator).validate(eq(QuerySpec.EDC_QUERY_SPEC_TYPE), isA(JsonObject.class));
            verifyNoInteractions(assetService);
        }
    }
}