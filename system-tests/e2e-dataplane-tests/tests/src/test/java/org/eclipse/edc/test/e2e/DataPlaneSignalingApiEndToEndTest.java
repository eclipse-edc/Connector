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
import org.eclipse.edc.connector.api.signaling.transform.SignalingApiTransformerRegistry;
import org.eclipse.edc.connector.api.signaling.transform.SignalingApiTransformerRegistryImpl;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowStartMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.core.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage.DATA_FLOW_TERMINATE_MESSAGE_REASON;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@EndToEndTest
public class DataPlaneSignalingApiEndToEndTest extends AbstractDataPlaneTest {

    private static final String DATAPLANE_PUBLIC_ENDPOINT_URL = "http://fizz.buzz/bar";

    protected final Duration timeout = Duration.ofSeconds(60);
    private ObjectMapper mapper;
    private SignalingApiTransformerRegistry registry;

    @BeforeEach
    void setup() {
        // this registry is entirely separate from the one that is included in the runtime
        registry = new SignalingApiTransformerRegistryImpl(new TypeTransformerRegistryImpl());
        var builderFactory = Json.createBuilderFactory(Map.of());
        mapper = JacksonJsonLd.createObjectMapper();
        registry.register(new JsonObjectFromDataFlowStartMessageTransformer(builderFactory, mapper));
        registry.register(new JsonObjectFromDataAddressTransformer(builderFactory, mapper));
        registry.register(new JsonObjectToDataAddressTransformer());
    }

    @DisplayName("Verify the POST /v1/dataflows endpoint returns the correct EDR")
    @Test
    void startTransfer() throws JsonProcessingException {
        var generator = runtime.getContext().getService(PublicEndpointGeneratorService.class);
        generator.addGeneratorFunction("HttpData", dataAddress -> Endpoint.url(DATAPLANE_PUBLIC_ENDPOINT_URL));

        var processId = "test-processId";
        var flowMessage = createStartMessage(processId);
        var jo = registry.transform(flowMessage, JsonObject.class).orElseThrow(failTest());

        var resultJson = DATAPLANE.initiateTransfer(jo);
        var dataAddress = registry.transform(mapper.readValue(resultJson, JsonObject.class), DataAddress.class)
                .orElseThrow(failTest());

        // verify basic shape of the DSPACE data address (=EDR token)
        assertThat(dataAddress).isNotNull();
        assertThat(dataAddress.getType()).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
        assertThat(dataAddress.getProperties())
                .containsKey("authorization")
                .containsEntry("endpoint", DATAPLANE_PUBLIC_ENDPOINT_URL)
                .containsEntry("authType", "bearer");

        // verify that the data flow was created
        var store = runtime.getService(DataPlaneStore.class).findById(processId);
        assertThat(store).isNotNull();
    }

    @DisplayName("Verify that GET /v1/dataflows/{id}/state returns the correct state")
    @Test
    void getState() {
        var dataFlowId = "test-flowId";

        var flow = DataFlow.Builder.newInstance()
                .id(dataFlowId)
                .state(DataFlowStates.STARTED.code())
                .build();
        runtime.getService(DataPlaneStore.class).save(flow);

        var resultJson = DATAPLANE.getDataPlaneSignalingApi()
                .baseRequest()
                .contentType(ContentType.JSON)
                .get("/v1/dataflows/%s/state".formatted(dataFlowId))
                .then()
                .body(notNullValue())
                .statusCode(200)
                .extract().response().jsonPath();

        assertThat(resultJson.getString("state")).isEqualToIgnoringCase("STARTED");
    }

    @DisplayName("Verify that POST /v1/dataflows/{id}/terminate terminates the flow, with an optional message")
    @Test
    void terminate() {
        var dataFlowId = "test-flowId";

        var flow = DataFlow.Builder.newInstance()
                .id(dataFlowId)
                .source(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://foo.bar/").build())
                .destination(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://fizz.buzz").build())
                .traceContext(Map.of())
                .state(DataFlowStates.STARTED.code())
                .build();
        var store = runtime.getService(DataPlaneStore.class);
        store.save(flow);

        var terminateMessage = Json.createObjectBuilder()
                .add(TYPE, DataFlowTerminateMessage.DATA_FLOW_TERMINATE_MESSAGE_TYPE)
                .add(DATA_FLOW_TERMINATE_MESSAGE_REASON, "test-reason")
                .build();

        DATAPLANE.getDataPlaneSignalingApi()
                .baseRequest()
                .body(terminateMessage)
                .contentType(ContentType.JSON)
                .post("/v1/dataflows/%s/terminate".formatted(dataFlowId))
                .then()
                .log().ifError()
                .statusCode(anyOf(equalTo(200), equalTo(204)));


        assertThat(store.findById(dataFlowId)).isNotNull()
                .extracting(DataFlow::getState)
                .isEqualTo(DataFlowStates.TERMINATED.code());

    }

    private DataFlowStartMessage createStartMessage(String processId) {
        return DataFlowStartMessage.Builder.newInstance()
                .processId(processId)
                .sourceDataAddress(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://foo.bar/").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://fizz.buzz").build())
                .flowType(FlowType.PULL)
                .participantId("some-participantId")
                .assetId("test-asset")
                .callbackAddress(URI.create("https://foo.bar/callback"))
                .agreementId("test-agreement")
                .build();
    }

    @NotNull
    private Function<Failure, AssertionError> failTest() {
        return f -> new AssertionError(f.getFailureDetail());
    }
}
