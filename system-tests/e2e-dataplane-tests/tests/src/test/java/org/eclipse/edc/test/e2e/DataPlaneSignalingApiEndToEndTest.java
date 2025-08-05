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

package org.eclipse.edc.test.e2e;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowStartMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowResponseMessageTransformer;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.dspace.from.JsonObjectFromDataAddressDspaceTransformer;
import org.eclipse.edc.transform.transformer.dspace.to.JsonObjectToDataAddressDspaceTransformer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage.DATA_FLOW_TERMINATE_MESSAGE_REASON;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EndToEndTest
public class DataPlaneSignalingApiEndToEndTest extends AbstractDataPlaneTest {

    private static final String DATAPLANE_PUBLIC_ENDPOINT_URL = "http://public/endpoint";
    private final TypeTransformerRegistry registry = new TypeTransformerRegistryImpl();
    private final TypeManager typeManager = mock();
    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        var builderFactory = Json.createBuilderFactory(Map.of());
        mapper = JacksonJsonLd.createObjectMapper();
        registry.register(new JsonObjectFromDataFlowStartMessageTransformer(builderFactory, typeManager, "test"));
        registry.register(new JsonObjectFromDataAddressDspaceTransformer(builderFactory, typeManager, "test"));
        registry.register(new JsonObjectToDataAddressDspaceTransformer());
        registry.register(new JsonObjectToDataFlowResponseMessageTransformer());
        when(typeManager.getMapper("test")).thenReturn(mapper);
        runtime.getService(PublicEndpointGeneratorService.class)
                .addGeneratorFunction("HttpData", address -> Endpoint.url(DATAPLANE_PUBLIC_ENDPOINT_URL));
        runtime.getService(PublicEndpointGeneratorService.class)
                .addGeneratorFunction("HttpData", () -> Endpoint.url(DATAPLANE_PUBLIC_ENDPOINT_URL + "/responseChannel"));
    }

    @DisplayName("Verify the POST /v1/dataflows endpoint returns the correct EDR (PULL)")
    @Test
    void startTransfer_pull() throws JsonProcessingException {
        seedVault();
        var jsonLd = runtime.getService(JsonLd.class);

        var processId = UUID.randomUUID().toString();
        var flowMessage = DataFlowStartMessage.Builder.newInstance()
                .processId(processId)
                .sourceDataAddress(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://foo.bar/").build())
                .transferType(new TransferType("HttpData", FlowType.PULL))
                .participantId("some-participantId")
                .assetId("test-asset")
                .callbackAddress(URI.create("https://foo.bar/callback"))
                .agreementId("test-agreement")
                .build();
        var startMessage = registry.transform(flowMessage, JsonObject.class).orElseThrow(failTest());

        var resultJson = DATAPLANE.baseControlRequest()
                .contentType(ContentType.JSON)
                .body(startMessage)
                .post("/v1/dataflows")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(notNullValue())
                .extract().body().asString();

        var dataFlowResponseMessage = jsonLd.expand(mapper.readValue(resultJson, JsonObject.class))
                .compose(json -> registry.transform(json, DataFlowResponseMessage.class))
                .orElseThrow(failTest());

        var dataAddress = dataFlowResponseMessage.getDataAddress();

        // verify basic shape of the DSPACE data address (=EDR token)
        assertThat(dataAddress).isNotNull();
        assertThat(dataAddress.getType()).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
        assertThat(dataAddress.getStringProperty("endpoint")).isEqualTo(DATAPLANE_PUBLIC_ENDPOINT_URL);
        assertThat(dataAddress.getStringProperty("authorization")).isNotNull();
        assertThat(dataAddress.getStringProperty("authType")).isEqualTo("bearer");

        // verify that the data flow was created
        var store = runtime.getService(DataPlaneStore.class).findById(processId);
        assertThat(store).isNotNull();
    }

    @DisplayName("Verify the POST /v1/dataflows endpoint returns the correct EDR (PUSH)")
    @Test
    void startTransfer_push() throws JsonProcessingException {
        seedVault();
        var jsonLd = runtime.getService(JsonLd.class);

        var processId = UUID.randomUUID().toString();
        var flowMessage = DataFlowStartMessage.Builder.newInstance()
                .processId(processId)
                .sourceDataAddress(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://foo.bar/").build())
                .transferType(new TransferType("HttpData", FlowType.PUSH))
                .destinationDataAddress(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://bar.baz/").build())
                .participantId("some-participantId")
                .assetId("test-asset")
                .callbackAddress(URI.create("https://foo.bar/callback"))
                .agreementId("test-agreement")
                .build();
        var startMessage = registry.transform(flowMessage, JsonObject.class).orElseThrow(failTest());

        var resultJson = DATAPLANE.baseControlRequest()
                .contentType(ContentType.JSON)
                .body(startMessage)
                .post("/v1/dataflows")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(notNullValue())
                .extract().body().asString();

        var dataFlowResponseMessage = jsonLd.expand(mapper.readValue(resultJson, JsonObject.class))
                .compose(json -> registry.transform(json, DataFlowResponseMessage.class))
                .orElseThrow(failTest());

        var dataAddress = dataFlowResponseMessage.getDataAddress();

        // should not return an EDR if transfer type is PUSH and no back-channel is provided
        assertThat(dataAddress).isNull();

        // verify that the data flow was created
        var store = runtime.getService(DataPlaneStore.class).findById(processId);
        assertThat(store).isNotNull();
    }

    @DisplayName("Verify the POST /v1/dataflows endpoint returns the correct EDR with a back-channel (PULL)")
    @Test
    void startTransfer_pull_withBidirectional() throws JsonProcessingException {
        seedVault();
        var jsonLd = runtime.getService(JsonLd.class);

        var processId = UUID.randomUUID().toString();

        var sourceDataAddress = DataAddress.Builder.newInstance()
                .type("HttpData")
                .property(EDC_NAMESPACE + "baseUrl", "http://foo.bar/")
                .responseChannel(DataAddress.Builder.newInstance()
                        .type("HttpData")
                        .property(EDC_NAMESPACE + "baseUrl", "http://foo.bar/responseChannel")
                        .build())
                .build();
        var flowMessage = DataFlowStartMessage.Builder.newInstance()
                .processId(processId)
                .sourceDataAddress(sourceDataAddress)
                .transferType(new TransferType("HttpData", FlowType.PULL, "HttpData"))
                .participantId("some-participantId")
                .assetId("test-asset")
                .callbackAddress(URI.create("https://foo.bar/callback"))
                .agreementId("test-agreement")
                .build();
        var startMessage = registry.transform(flowMessage, JsonObject.class).orElseThrow(failTest());

        var resultJson = DATAPLANE.baseControlRequest()
                .contentType(ContentType.JSON)
                .body(startMessage)
                .post("/v1/dataflows")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(notNullValue())
                .extract().body().asString();

        var dataFlowResponseMessage = jsonLd.expand(mapper.readValue(resultJson, JsonObject.class))
                .compose(json -> registry.transform(json, DataFlowResponseMessage.class))
                .orElseThrow(failTest());

        var dataAddress = dataFlowResponseMessage.getDataAddress();

        // verify basic shape of the DSPACE data address (=EDR token)
        assertThat(dataAddress).isNotNull();
        assertThat(dataAddress.getType()).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
        assertThat(dataAddress.getStringProperty("endpoint")).isEqualTo(DATAPLANE_PUBLIC_ENDPOINT_URL);
        assertThat(dataAddress.getStringProperty("authorization")).isNotNull();
        assertThat(dataAddress.getStringProperty("authType")).isEqualTo("bearer");
        assertThat(dataAddress.getStringProperty("responseChannel-endpoint")).isEqualTo(DATAPLANE_PUBLIC_ENDPOINT_URL + "/responseChannel");
        assertThat(dataAddress.getStringProperty("responseChannel-endpointType")).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
        assertThat(dataAddress.getStringProperty("responseChannel-authType")).isEqualTo("bearer");
        assertThat(dataAddress.getStringProperty("responseChannel-authorization")).isNotNull();

        // verify that the data flow was created
        var store = runtime.getService(DataPlaneStore.class).findById(processId);
        assertThat(store).isNotNull();
    }

    @DisplayName("Verify the POST /v1/dataflows endpoint returns the correct EDR with a back-channel (PUSH)")
    @Test
    void startTransfer_push_withBidirectional() throws JsonProcessingException {
        seedVault();
        var jsonLd = runtime.getService(JsonLd.class);

        var processId = UUID.randomUUID().toString();
        var sourceDataAddress = DataAddress.Builder.newInstance()
                .type("HttpData")
                .property(EDC_NAMESPACE + "baseUrl", "http://foo.bar/")
                .responseChannel(DataAddress.Builder.newInstance()
                        .type("HttpData")
                        .property(EDC_NAMESPACE + "baseUrl", "http://foo.bar/responseChannel")
                        .build())
                .build();
        var flowMessage = DataFlowStartMessage.Builder.newInstance()
                .processId(processId)
                .sourceDataAddress(sourceDataAddress)
                .destinationDataAddress(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://bar.baz/").build())
                .transferType(new TransferType("HttpData", FlowType.PUSH, "HttpData"))
                .participantId("some-participantId")
                .assetId("test-asset")
                .callbackAddress(URI.create("https://foo.bar/callback"))
                .agreementId("test-agreement")
                .build();
        var startMessage = registry.transform(flowMessage, JsonObject.class).orElseThrow(failTest());

        var resultJson = DATAPLANE.baseControlRequest()
                .contentType(ContentType.JSON)
                .body(startMessage)
                .post("/v1/dataflows")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(notNullValue())
                .extract().body().asString();

        var dataFlowResponseMessage = jsonLd.expand(mapper.readValue(resultJson, JsonObject.class))
                .compose(json -> registry.transform(json, DataFlowResponseMessage.class))
                .orElseThrow(failTest());

        var dataAddress = dataFlowResponseMessage.getDataAddress();

        // verify basic shape of the DSPACE data address (=EDR token)
        assertThat(dataAddress).isNotNull();
        assertThat(dataAddress.getType()).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
        assertThat(dataAddress.getStringProperty("endpoint")).isEqualTo(DATAPLANE_PUBLIC_ENDPOINT_URL);
        assertThat(dataAddress.getStringProperty("authorization")).isNotNull();
        assertThat(dataAddress.getStringProperty("authType")).isEqualTo("bearer");
        assertThat(dataAddress.getStringProperty("responseChannel-endpoint")).isEqualTo(DATAPLANE_PUBLIC_ENDPOINT_URL + "/responseChannel");
        assertThat(dataAddress.getStringProperty("responseChannel-endpointType")).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
        assertThat(dataAddress.getStringProperty("responseChannel-authType")).isEqualTo("bearer");
        assertThat(dataAddress.getStringProperty("responseChannel-authorization")).isNotNull();

        // verify that the data flow was created
        var store = runtime.getService(DataPlaneStore.class).findById(processId);
        assertThat(store).isNotNull();
    }

    @DisplayName("Verify that GET /v1/dataflows/{id}/state returns the correct state")
    @Test
    void getState() {
        seedVault();
        var dataFlowId = "test-flowId";

        var flow = DataFlow.Builder.newInstance()
                .id(dataFlowId)
                .state(DataFlowStates.STARTED.code())
                .build();
        runtime.getService(DataPlaneStore.class).save(flow);

        var resultJson = DATAPLANE.baseControlRequest()
                .contentType(ContentType.JSON)
                .get("/v1/dataflows/%s/state" .formatted(dataFlowId))
                .then()
                .body(notNullValue())
                .statusCode(200)
                .extract().response().jsonPath();

        assertThat(resultJson.getString("state")).isEqualToIgnoringCase("STARTED");
    }

    @DisplayName("Verify that POST /v1/dataflows/{id}/terminate terminates the flow, with an optional message")
    @Test
    void terminate() {
        seedVault();
        var dataFlowId = "test-flowId";

        var flow = DataFlow.Builder.newInstance()
                .id(dataFlowId)
                .source(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://foo.bar/").build())
                .destination(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://fizz.buzz").build())
                .transferType(new TransferType("HttpData", FlowType.PUSH))
                .traceContext(Map.of())
                .state(DataFlowStates.STARTED.code())
                .build();
        var store = runtime.getService(DataPlaneStore.class);
        store.save(flow);

        var terminateMessage = Json.createObjectBuilder()
                .add(TYPE, DataFlowTerminateMessage.DATA_FLOW_TERMINATE_MESSAGE_TYPE)
                .add(DATA_FLOW_TERMINATE_MESSAGE_REASON, "test-reason")
                .build();

        DATAPLANE.baseControlRequest()
                .body(terminateMessage)
                .contentType(ContentType.JSON)
                .post("/v1/dataflows/%s/terminate" .formatted(dataFlowId))
                .then()
                .log().ifError()
                .statusCode(anyOf(equalTo(200), equalTo(204)));


        assertThat(store.findById(dataFlowId)).isNotNull()
                .extracting(DataFlow::getState)
                .isEqualTo(DataFlowStates.TERMINATED.code());

    }

    @NotNull
    private Function<Failure, AssertionError> failTest() {
        return f -> new AssertionError(f.getFailureDetail());
    }
}
