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

package org.eclipse.edc.connector.controlplane.api.management.asset;

import io.restassured.specification.RequestSpecification;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public abstract class BaseAssetApiControllerTest extends RestControllerTestBase {

    private static final String TEST_ASSET_ID = "test-asset-id";
    private static final String TEST_ASSET_CONTENTTYPE = "application/json";
    private static final String TEST_ASSET_DESCRIPTION = "test description";
    private static final String TEST_ASSET_VERSION = "0.4.2";
    private static final String TEST_ASSET_NAME = "test-asset";
    protected final AssetService service = mock(AssetService.class);
    protected final TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);
    protected final JsonObjectValidatorRegistry validator = mock(JsonObjectValidatorRegistry.class);
    protected final SingleParticipantContextSupplier participantContextSupplier = () -> new ParticipantContext("participantId");

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
    }

    @Test
    void requestAsset() {
        when(service.search(any()))
                .thenReturn(ServiceResult.success(List.of(Asset.Builder.newInstance().build())));
        when(transformerRegistry.transform(isA(Asset.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createAssetJson().build()));
        when(transformerRegistry.transform(isA(JsonObject.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

        baseRequest()
                .contentType(JSON)
                .body("{}")
                .post("/assets/request")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
        verify(service).search(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(isA(Asset.class), eq(JsonObject.class));
        verify(transformerRegistry).transform(isA(JsonObject.class), eq(QuerySpec.class));
    }

    @Test
    void requestAsset_filtersOutFailedTransforms() {
        when(service.search(any()))
                .thenReturn(ServiceResult.success(List.of(Asset.Builder.newInstance().build())));
        when(transformerRegistry.transform(isA(JsonObject.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(transformerRegistry.transform(isA(Asset.class), eq(JsonObject.class)))
                .thenReturn(Result.failure("failed to transform"));
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

        baseRequest()
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
        when(service.search(any())).thenReturn(ServiceResult.badRequest("test-message"));
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

        baseRequest()
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
        when(service.search(any())).thenReturn(ServiceResult.success());
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

        baseRequest()
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
        when(service.search(any())).thenReturn(ServiceResult.badRequest());
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

        baseRequest()
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

        baseRequest()
                .contentType(JSON)
                .body("{}")
                .post("/assets/request")
                .then()
                .statusCode(400);
        verify(validator).validate(eq(QuerySpec.EDC_QUERY_SPEC_TYPE), isA(JsonObject.class));
        verifyNoInteractions(service);
    }

    @Test
    void getSingleAsset() {
        var asset = Asset.Builder.newInstance().property("key", "value").build();
        when(service.findById("id")).thenReturn(asset);
        var assetJson = createAssetJson().build();
        when(transformerRegistry.transform(isA(Asset.class), eq(JsonObject.class))).thenReturn(Result.success(assetJson));

        baseRequest()
                .get("/assets/id")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(ID, equalTo(TEST_ASSET_ID));

        verify(transformerRegistry).transform(isA(Asset.class), eq(JsonObject.class));
        verifyNoMoreInteractions(transformerRegistry);
    }

    @Test
    void getSingleAsset_notFound() {
        when(service.findById(any())).thenReturn(null);

        baseRequest()
                .get("/assets/not-existent-id")
                .then()
                .statusCode(404);
    }

    @Test
    void getAssetById_shouldReturnNotFound_whenTransformFails() {
        when(service.findById("id")).thenReturn(Asset.Builder.newInstance().build());
        when(transformerRegistry.transform(isA(Asset.class), eq(JsonObject.class))).thenReturn(Result.failure("failure"));

        baseRequest()
                .get("/assets/id")
                .then()
                .statusCode(500);
    }

    @Test
    void createAsset() {
        var asset = createAssetBuilder().dataAddress(DataAddress.Builder.newInstance().type("any").build()).build();
        when(transformerRegistry.transform(any(JsonObject.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(service.create(isA(Asset.class))).thenReturn(ServiceResult.success(asset));
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

        baseRequest()
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
        verify(service).create(isA(Asset.class));
        verifyNoMoreInteractions(service, transformerRegistry);
    }

    @Test
    public void createAsset_shouldReturnBadRequest_whenValidationFails() {
        when(validator.validate(any(), any())).thenReturn(ValidationResult.failure(violation("a failure", "a path")));

        baseRequest()
                .contentType(JSON)
                .body(createAssetJson().build())
                .post("/assets")
                .then()
                .statusCode(400);

        verify(validator).validate(eq(EDC_ASSET_TYPE), isA(JsonObject.class));
        verifyNoInteractions(service, transformerRegistry);
    }

    @Test
    void createAsset_shouldReturnBadRequest_whenTransformFails() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(Asset.class))).thenReturn(Result.failure("failed"));
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

        baseRequest()
                .body(createAssetJson().build())
                .contentType(JSON)
                .post("/assets")
                .then()
                .statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    void createAsset_alreadyExists() {
        var asset = createAssetBuilder().dataAddress(DataAddress.Builder.newInstance().type("any").build()).build();
        when(transformerRegistry.transform(any(JsonObject.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(service.create(isA(Asset.class))).thenReturn(ServiceResult.conflict("already exists"));
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

        baseRequest()
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

        baseRequest()
                .body(createAssetJson().build())
                .contentType(JSON)
                .post("/assets")
                .then()
                .statusCode(400);
    }

    @Test
    void deleteAsset() {
        when(service.delete("assetId"))
                .thenReturn(ServiceResult.success(createAssetBuilder().build()));

        baseRequest()
                .contentType(JSON)
                .delete("/assets/assetId")
                .then()
                .statusCode(204);
        verify(service).delete("assetId");
    }

    @Test
    void deleteAsset_notExists() {
        when(service.delete(any())).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .contentType(JSON)
                .delete("/assets/not-existent-id")
                .then()
                .statusCode(404);
    }

    @Test
    void deleteAsset_conflicts() {
        when(service.delete(any())).thenReturn(ServiceResult.conflict("conflict"));

        baseRequest()
                .contentType(JSON)
                .delete("/assets/id")
                .then()
                .statusCode(409);
    }

    @Test
    void updateAsset_whenExists() {
        var asset = Asset.Builder.newInstance().property("key1", "value1").build();
        when(transformerRegistry.transform(isA(JsonObject.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(service.update(any(Asset.class))).thenReturn(ServiceResult.success());
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

        baseRequest()
                .body(createAssetJson().build())
                .contentType(JSON)
                .put("/assets")
                .then()
                .statusCode(204);
        verify(service).update(eq(asset));
    }

    @Test
    void updateAsset_shouldReturnNotFound_whenItDoesNotExists() {
        var asset = Asset.Builder.newInstance().property("key1", "value1").build();
        when(transformerRegistry.transform(isA(JsonObject.class), eq(Asset.class))).thenReturn(Result.success(asset));
        when(service.update(any(Asset.class))).thenReturn(ServiceResult.notFound("not found"));
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

        baseRequest()
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

        baseRequest()
                .body(createAssetJson().build())
                .contentType(JSON)
                .put("/assets")
                .then()
                .statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    public void updateAsset_shouldReturnBadRequest_whenValidationFails() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(Asset.class))).thenReturn(Result.failure("error"));
        when(validator.validate(any(), any())).thenReturn(ValidationResult.failure(violation("validation failure", "path")));

        baseRequest()
                .body(createAssetJson().build())
                .contentType(JSON)
                .put("/assets")
                .then()
                .statusCode(400);
        verify(validator).validate(eq(EDC_ASSET_TYPE), isA(JsonObject.class));
        verifyNoInteractions(service, transformerRegistry);
    }

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

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/" + versionPath())
                .when();
    }

    protected abstract String versionPath();

    private Asset.Builder createAssetBuilder() {
        return Asset.Builder.newInstance()
                .name(TEST_ASSET_NAME)
                .id(TEST_ASSET_ID)
                .contentType(TEST_ASSET_CONTENTTYPE)
                .description(TEST_ASSET_DESCRIPTION)
                .version(TEST_ASSET_VERSION);
    }
}
