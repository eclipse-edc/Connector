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

package org.eclipse.edc.connector.dataplane.api.controller.v1;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_TYPE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage.EDC_DATA_FLOW_START_MESSAGE_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class DataPlaneSignalingApiControllerTest extends RestControllerTestBase {

    private final TypeTransformerRegistry transformerRegistry = mock();
    private final DataPlaneManager dataplaneManager = mock();

    @Nested
    class Start {

        @Test
        void shouldStartDataFlow() {
            var flowStartMessage = createFlowStartMessage();
            var flowResponse = DataFlowResponseMessage.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("test-edr").build()).build();
            when(transformerRegistry.transform(isA(JsonObject.class), eq(DataFlowStartMessage.class)))
                    .thenReturn(success(flowStartMessage));
            when(dataplaneManager.validate(any())).thenReturn(success());
            when(dataplaneManager.start(any()))
                    .thenReturn(StatusResult.success(flowResponse));
            when(transformerRegistry.transform(isA(DataFlowResponseMessage.class), eq(JsonObject.class)))
                    .thenReturn(success(Json.createObjectBuilder().add("foo", "bar").build()));
            when(transformerRegistry.transform(isA(DataAddress.class), eq(JsonObject.class)))
                    .thenReturn(success(Json.createObjectBuilder().add("foo", "bar").build()));

            var result = baseRequest()
                    .contentType(ContentType.JSON)
                    .body(inputWithType(EDC_DATA_FLOW_START_MESSAGE_TYPE))
                    .post("/v1/dataflows")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .extract().body().as(JsonObject.class);

            assertThat(result).hasEntrySatisfying("foo", val -> assertThat(((JsonString) val).getString()).isEqualTo("bar"));
            verify(dataplaneManager).start(eq(flowStartMessage));
        }

        @Test
        void shouldProvisionDataFlow() {
            var provisionMessage = createFlowProvisionMessage();
            var flowResponse = DataFlowResponseMessage.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("test-edr").build()).build();
            when(transformerRegistry.transform(isA(JsonObject.class), eq(DataFlowProvisionMessage.class)))
                    .thenReturn(success(provisionMessage));
            when(dataplaneManager.provision(any())).thenReturn(StatusResult.success(flowResponse));
            when(transformerRegistry.transform(isA(DataFlowResponseMessage.class), eq(JsonObject.class)))
                    .thenReturn(success(Json.createObjectBuilder().add("foo", "bar").build()));

            var result = baseRequest()
                    .contentType(ContentType.JSON)
                    .body(inputWithType(EDC_DATA_FLOW_PROVISION_MESSAGE_TYPE))
                    .post("/v1/dataflows")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .extract().body().as(JsonObject.class);

            assertThat(result).hasEntrySatisfying("foo", val -> assertThat(((JsonString) val).getString()).isEqualTo("bar"));
            verify(dataplaneManager).provision(same(provisionMessage));
        }

        @Test
        void shouldReturnBadRequest_whenUnsupportedType() {
            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(inputWithType("unsupported"))
                    .post("/v1/dataflows")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

            verifyNoInteractions(transformerRegistry, dataplaneManager);
        }

        @Test
        void shouldReturnBadRequest_whenInvalidMessage() {
            when(transformerRegistry.transform(isA(JsonObject.class), eq(DataFlowStartMessage.class)))
                    .thenReturn(success(createFlowStartMessage()));
            when(dataplaneManager.validate(any())).thenReturn(Result.failure("foobar"));

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(inputWithType(EDC_DATA_FLOW_START_MESSAGE_TYPE))
                    .post("/v1/dataflows")
                    .then()
                    .statusCode(400);

            verify(transformerRegistry).transform(isA(JsonObject.class), eq(DataFlowStartMessage.class));
            verify(dataplaneManager).validate(any(DataFlowStartMessage.class));
            verifyNoMoreInteractions(transformerRegistry, dataplaneManager);
        }

        @Test
        void shouldReturnBadRequest_whenTransformationFails() {
            when(transformerRegistry.transform(isA(JsonObject.class), eq(DataFlowStartMessage.class))).thenReturn(Result.failure("foo-bar"));

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(inputWithType(EDC_DATA_FLOW_START_MESSAGE_TYPE))
                    .post("/v1/dataflows")
                    .then()
                    .statusCode(400);

            verify(transformerRegistry).transform(isA(JsonObject.class), eq(DataFlowStartMessage.class));
            verifyNoMoreInteractions(transformerRegistry, dataplaneManager);
        }

        @Test
        void shouldReturnBadRequest_whenCreateEdrFails() {
            when(transformerRegistry.transform(isA(JsonObject.class), eq(DataFlowStartMessage.class)))
                    .thenReturn(success(createFlowStartMessage()));
            when(dataplaneManager.validate(any())).thenReturn(success());
            when(dataplaneManager.start(any()))
                    .thenReturn(StatusResult.failure(ERROR_RETRY, "test-failure"));

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(inputWithType(EDC_DATA_FLOW_START_MESSAGE_TYPE))
                    .post("/v1/dataflows")
                    .then()
                    .statusCode(400);

            verify(transformerRegistry).transform(isA(JsonObject.class), eq(DataFlowStartMessage.class));
            verify(dataplaneManager).validate(any());
            verify(dataplaneManager).start(any());
            verifyNoMoreInteractions(transformerRegistry, dataplaneManager);
        }

        @Test
        void shouldReturnInternalServerError_whenDataAddressTransformationFails() {
            var flowStartMessage = createFlowStartMessage();
            var flowResponse = DataFlowResponseMessage.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("test-edr").build()).build();

            when(transformerRegistry.transform(isA(JsonObject.class), eq(DataFlowStartMessage.class)))
                    .thenReturn(success(flowStartMessage));
            when(dataplaneManager.validate(any())).thenReturn(success());
            when(dataplaneManager.start(any()))
                    .thenReturn(StatusResult.success(flowResponse));

            when(transformerRegistry.transform(isA(DataAddress.class), eq(JsonObject.class)))
                    .thenReturn(failure("test-failure"));

            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(inputWithType(EDC_DATA_FLOW_START_MESSAGE_TYPE))
                    .post("/v1/dataflows")
                    .then()
                    .statusCode(500);

            verify(dataplaneManager).start(eq(flowStartMessage));
        }

        private String inputWithType(String type) {
            return Json.createObjectBuilder()
                    .add(TYPE, Json.createArrayBuilder().add(type))
                    .build()
                    .toString();
        }
    }

    @DisplayName("Expect HTTP 200 and the correct response when getting the state")
    @Test
    void getTransferState() {
        var flowId = "test-id";
        when(dataplaneManager.getTransferState(eq(flowId))).thenReturn(DataFlowStates.RECEIVED);

        var state = baseRequest()
                .get("/v1/dataflows/%s/state".formatted(flowId))
                .then()
                .statusCode(200)
                .extract().as(JsonObject.class);
        assertThat(state.getString(EDC_NAMESPACE + "state")).isEqualTo("RECEIVED");
    }

    @DisplayName("Expect HTTP 204 when DataFlow is terminated successfully")
    @Test
    void terminate() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(DataFlowTerminateMessage.class)))
                .thenReturn(success(DataFlowTerminateMessage.Builder.newInstance().reason("test-reason").build()));
        var flowId = "test-id";
        when(dataplaneManager.terminate(eq(flowId), any())).thenReturn(StatusResult.success());

        var jsonObject = Json.createObjectBuilder().build();
        baseRequest()
                .contentType(ContentType.JSON)
                .body(jsonObject)
                .post("/v1/dataflows/%s/terminate".formatted(flowId))
                .then()
                .statusCode(204);
    }

    @DisplayName("Expect HTTP 400 when DataFlow is cannot be terminated")
    @Test
    void terminate_whenCannotTerminate() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(DataFlowTerminateMessage.class)))
                .thenReturn(success(DataFlowTerminateMessage.Builder.newInstance().reason("test-reason").build()));
        var flowId = "test-id";
        when(dataplaneManager.terminate(eq(flowId), any())).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR));

        var jsonObject = Json.createObjectBuilder().build();
        baseRequest()
                .contentType(ContentType.JSON)
                .body(jsonObject)
                .post("/v1/dataflows/%s/terminate".formatted(flowId))
                .then()
                .statusCode(400);
    }

    @Deprecated(since = "0.15.0")
    @Nested
    class Suspend {

        @Test
        void shouldReturn204_whenDataFlowIsSuspended() {
            when(transformerRegistry.transform(isA(JsonObject.class), eq(DataFlowTerminateMessage.class)))
                    .thenReturn(success(DataFlowTerminateMessage.Builder.newInstance().reason("test-reason").build()));
            var flowId = "test-id";
            when(dataplaneManager.terminate(eq(flowId), any())).thenReturn(StatusResult.success());

            var jsonObject = Json.createObjectBuilder().build();
            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(jsonObject)
                    .post("/v1/dataflows/%s/suspend".formatted(flowId))
                    .then()
                    .statusCode(204);
        }

        @Test
        void shouldReturn500_whenDataFlowCannotBeSuspended() {
            when(transformerRegistry.transform(isA(JsonObject.class), eq(DataFlowTerminateMessage.class)))
                    .thenReturn(success(DataFlowTerminateMessage.Builder.newInstance().reason("test-reason").build()));
            var flowId = "test-id";
            when(dataplaneManager.terminate(eq(flowId), any())).thenReturn(StatusResult.failure(ResponseStatus.FATAL_ERROR));

            var jsonObject = Json.createObjectBuilder().build();
            baseRequest()
                    .contentType(ContentType.JSON)
                    .body(jsonObject)
                    .post("/v1/dataflows/%s/suspend".formatted(flowId))
                    .then()
                    .statusCode(400);
        }

    }

    @Nested
    class CheckAvailability {
        @Test
        void shouldReturn204_whenDataPlaneIsAvailable() {
            baseRequest()
                    .get("/v1/dataflows/check")
                    .then()
                    .statusCode(204);
        }
    }

    @Override
    protected Object controller() {
        return new DataPlaneSignalingApiController(transformerRegistry, dataplaneManager, mock());
    }

    private DataFlowStartMessage createFlowStartMessage() {
        return DataFlowStartMessage.Builder.newInstance()
                .processId("processId")
                .assetId("assetId")
                .agreementId("agreementId")
                .participantId("participantId")
                .flowType(FlowType.PULL)
                .callbackAddress(URI.create("http://localhost"))
                .sourceDataAddress(DataAddress.Builder.newInstance().type("sourceType").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("destType").build())
                .build();
    }

    private DataFlowProvisionMessage createFlowProvisionMessage() {
        return DataFlowProvisionMessage.Builder.newInstance()
                .processId("processId")
                .assetId("assetId")
                .agreementId("agreementId")
                .participantId("participantId")
                .transferType(new TransferType("any", FlowType.PULL))
                .callbackAddress(URI.create("http://localhost"))
                .destination(DataAddress.Builder.newInstance().type("destType").build())
                .build();
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .when();
    }
}
