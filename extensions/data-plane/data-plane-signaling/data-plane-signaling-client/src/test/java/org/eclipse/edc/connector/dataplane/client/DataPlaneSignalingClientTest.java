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

package org.eclipse.edc.connector.dataplane.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.signaling.transform.SignalingApiTransformerRegistryImpl;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowResponseMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowStartMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowSuspendMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowTerminateMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowResponseMessageTransformer;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.response.TransferErrorResponse;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testHttpClient;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.CONFLICT_409;
import static org.mockserver.model.HttpStatusCode.NO_CONTENT_204;
import static org.mockserver.stop.Stop.stopQuietly;

@ComponentTest
class DataPlaneSignalingClientTest {

    private static final ObjectMapper MAPPER = createObjectMapper();

    private static final int DATA_PLANE_API_PORT = getFreePort();
    private static final String DATA_PLANE_PATH = "/v1/dataflows";
    private static final String DATA_PLANE_API_URI = "http://localhost:" + DATA_PLANE_API_PORT + DATA_PLANE_PATH;

    private static final TypeTransformerRegistry underlyingRegistry = mock();
    private static final TypeTransformerRegistry transformerRegistry = new SignalingApiTransformerRegistryImpl(underlyingRegistry);
    private static final TypeTransformer<DataAddress, JsonObject> fromDataAddressTransformer = mock();
    private static final TypeTransformer<JsonObject, DataAddress> toDataAddressTransformer = mock();
    private static final TitaniumJsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));
    private static ClientAndServer dataPlane;
    private final DataPlaneInstance instance = DataPlaneInstance.Builder.newInstance().url(DATA_PLANE_API_URI).build();
    private final DataPlaneClient dataPlaneClient = new DataPlaneSignalingClient(testHttpClient(), transformerRegistry, jsonLd, MAPPER, instance);

    @BeforeAll
    public static void setUp() {
        var factory = Json.createBuilderFactory(Map.of());

        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE);
        dataPlane = startClientAndServer(DATA_PLANE_API_PORT);
        transformerRegistry.register(new JsonObjectFromDataFlowTerminateMessageTransformer(factory));
        transformerRegistry.register(new JsonObjectFromDataFlowSuspendMessageTransformer(factory));
        transformerRegistry.register(new JsonObjectFromDataFlowStartMessageTransformer(factory, MAPPER));
        transformerRegistry.register(new JsonObjectFromDataFlowResponseMessageTransformer(factory));
        transformerRegistry.register(new JsonObjectToDataFlowResponseMessageTransformer());

        when(underlyingRegistry.transformerFor(any(DataAddress.class), eq(JsonObject.class))).thenReturn(fromDataAddressTransformer);
        when(underlyingRegistry.transformerFor(any(JsonObject.class), eq(DataAddress.class))).thenReturn(toDataAddressTransformer);
        when(fromDataAddressTransformer.transform(any(DataAddress.class), any())).thenReturn(Json.createObjectBuilder().build());
        when(toDataAddressTransformer.transform(any(JsonObject.class), any())).thenReturn(DataAddress.Builder.newInstance().type("type").build());
    }

    @AfterAll
    public static void tearDown() {
        stopQuietly(dataPlane);
    }

    private static HttpResponse withResponse(String errorMsg) throws JsonProcessingException {
        return response().withStatusCode(HttpStatusCode.BAD_REQUEST_400.code())
                .withBody(MAPPER.writeValueAsString(new TransferErrorResponse(List.of(errorMsg))), MediaType.APPLICATION_JSON);
    }

    private static DataFlowStartMessage createDataFlowRequest() {
        return DataFlowStartMessage.Builder.newInstance()
                .id("123")
                .processId("456")
                .flowType(FlowType.PULL)
                .assetId("assetId")
                .agreementId("agreementId")
                .participantId("participantId")
                .callbackAddress(URI.create("http://void"))
                .sourceDataAddress(DataAddress.Builder.newInstance().type("test").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("test").build())
                .build();
    }

    @AfterEach
    public void resetMockServer() {
        dataPlane.reset();
    }

    @Test
    void start_verifyReturnFatalErrorIfReceiveResponseWithNullBody() throws JsonProcessingException {
        var flowRequest = createDataFlowRequest();

        var expected = transformerRegistry.transform(flowRequest, JsonObject.class)
                .compose(jsonLd::compact)
                .orElseThrow((e) -> new EdcException(e.getFailureDetail()));

        var httpRequest = new HttpRequest().withPath(DATA_PLANE_PATH).withBody(MAPPER.writeValueAsString(expected));
        dataPlane.when(httpRequest, once()).respond(response().withStatusCode(HttpStatusCode.BAD_REQUEST_400.code()));

        var result = dataPlaneClient.start(flowRequest);

        dataPlane.verify(httpRequest, VerificationTimes.once());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().status()).isEqualTo(ResponseStatus.FATAL_ERROR);
        assertThat(result.getFailureMessages())
                .anySatisfy(s -> assertThat(s)
                        .isEqualTo("Transfer request failed with status code 400 for request %s", flowRequest.getProcessId())
                );
    }

    @Test
    void start_verifyReturnFatalErrorIfReceiveErrorInResponse() throws JsonProcessingException {
        var flowRequest = createDataFlowRequest();

        var expected = transformerRegistry.transform(flowRequest, JsonObject.class)
                .compose(jsonLd::compact)
                .orElseThrow((e) -> new EdcException(e.getFailureDetail()));

        var httpRequest = new HttpRequest().withPath(DATA_PLANE_PATH).withBody(MAPPER.writeValueAsString(expected));
        var errorMsg = UUID.randomUUID().toString();
        dataPlane.when(httpRequest, once()).respond(withResponse(errorMsg));

        var result = dataPlaneClient.start(flowRequest);

        dataPlane.verify(httpRequest, VerificationTimes.once());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().status()).isEqualTo(ResponseStatus.FATAL_ERROR);
        assertThat(result.getFailureMessages())
                .anySatisfy(s -> assertThat(s)
                        .isEqualTo(format("Transfer request failed with status code 400 for request %s", flowRequest.getProcessId()))
                );
    }

    @Test
    void start_verifyReturnFatalErrorIfTransformFails() {
        var flowRequest = createDataFlowRequest();
        TypeTransformerRegistry registry = mock();
        var dataPlaneClient = new DataPlaneSignalingClient(testHttpClient(), registry, jsonLd, MAPPER, instance);

        when(registry.transform(any(), any())).thenReturn(Result.failure("Transform Failure"));

        var result = dataPlaneClient.start(flowRequest);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().status()).isEqualTo(ResponseStatus.FATAL_ERROR);
        assertThat(result.getFailureMessages())
                .anySatisfy(s -> assertThat(s)
                        .isEqualTo("Transform Failure")
                );
    }

    @Test
    void start_verifyReturnFatalError_whenBadResponse() throws JsonProcessingException {
        var flowRequest = createDataFlowRequest();
        var expected = transformerRegistry.transform(flowRequest, JsonObject.class)
                .compose(jsonLd::compact)
                .orElseThrow((e) -> new EdcException(e.getFailureDetail()));


        var httpRequest = new HttpRequest().withPath(DATA_PLANE_PATH).withBody(MAPPER.writeValueAsString(expected));
        dataPlane.when(httpRequest, once()).respond(response().withBody("{}").withStatusCode(HttpStatusCode.OK_200.code()));

        var result = dataPlaneClient.start(flowRequest);

        dataPlane.verify(httpRequest, VerificationTimes.once());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().status()).isEqualTo(ResponseStatus.FATAL_ERROR);
        assertThat(result.getFailureMessages())
                .anySatisfy(s -> assertThat(s)
                        .contains("Error expanding JSON-LD structure")
                );
    }

    @Test
    void start_verifyTransferSuccess() throws JsonProcessingException {
        var flowRequest = createDataFlowRequest();
        var expected = transformerRegistry.transform(flowRequest, JsonObject.class)
                .compose(jsonLd::compact)
                .orElseThrow((e) -> new EdcException(e.getFailureDetail()));

        var flowResponse = DataFlowResponseMessage.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("type").build()).build();
        var response = transformerRegistry.transform(flowResponse, JsonObject.class)
                .compose(jsonLd::compact)
                .orElseThrow((e) -> new EdcException(e.getFailureDetail()));


        var httpRequest = new HttpRequest().withPath(DATA_PLANE_PATH).withBody(MAPPER.writeValueAsString(expected));
        dataPlane.when(httpRequest, once()).respond(response().withBody(MAPPER.writeValueAsString(response)).withStatusCode(HttpStatusCode.OK_200.code()));

        var result = dataPlaneClient.start(flowRequest);

        dataPlane.verify(httpRequest, VerificationTimes.once());

        assertThat(result.succeeded()).isTrue();

        assertThat(result.getContent().getDataAddress()).isNotNull();
    }

    @Test
    void start_verifyTransferSuccess_withoutDataAddress() throws JsonProcessingException {
        var flowRequest = createDataFlowRequest();
        var expected = transformerRegistry.transform(flowRequest, JsonObject.class)
                .compose(jsonLd::compact)
                .orElseThrow((e) -> new EdcException(e.getFailureDetail()));

        var flowResponse = DataFlowResponseMessage.Builder.newInstance().build();
        var response = transformerRegistry.transform(flowResponse, JsonObject.class)
                .compose(jsonLd::compact)
                .orElseThrow((e) -> new EdcException(e.getFailureDetail()));


        var httpRequest = new HttpRequest().withPath(DATA_PLANE_PATH).withBody(MAPPER.writeValueAsString(expected));
        dataPlane.when(httpRequest, once()).respond(response().withBody(MAPPER.writeValueAsString(response)).withStatusCode(HttpStatusCode.OK_200.code()));

        var result = dataPlaneClient.start(flowRequest);

        dataPlane.verify(httpRequest, VerificationTimes.once());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getDataAddress()).isNull();
    }

    @Test
    void terminate_shouldCallTerminateOnAllTheAvailableDataPlanes() {
        var httpRequest = new HttpRequest().withMethod("POST").withPath(DATA_PLANE_PATH + "/processId/terminate");
        dataPlane.when(httpRequest, once()).respond(response().withStatusCode(NO_CONTENT_204.code()));

        var result = dataPlaneClient.terminate("processId");

        assertThat(result).isSucceeded();
        dataPlane.verify(httpRequest, VerificationTimes.once());
    }

    @Test
    void terminate_shouldFail_whenConflictResponse() {
        var httpRequest = new HttpRequest().withMethod("POST").withPath(DATA_PLANE_PATH + "/processId/terminate");
        dataPlane.when(httpRequest, once()).respond(response().withStatusCode(CONFLICT_409.code()));

        var result = dataPlaneClient.terminate("processId");

        assertThat(result).isFailed();
    }

    @Test
    void terminate_verifyReturnFatalErrorIfTransformFails() {
        TypeTransformerRegistry registry = mock();
        var dataPlaneClient = new DataPlaneSignalingClient(testHttpClient(), registry, jsonLd, MAPPER, instance);

        when(registry.transform(any(), any())).thenReturn(Result.failure("Transform Failure"));

        var result = dataPlaneClient.terminate("processId");

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().status()).isEqualTo(ResponseStatus.FATAL_ERROR);
        assertThat(result.getFailureMessages())
                .anySatisfy(s -> assertThat(s)
                        .isEqualTo("Transform Failure")
                );
    }
}
