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
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowProvisionMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowResponseMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowStartMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowSuspendMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowTerminateMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowResponseMessageTransformer;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.http.client.ControlApiHttpClientImpl;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseFailure;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.dspace.from.JsonObjectFromDataAddressDspaceTransformer;
import org.eclipse.edc.transform.transformer.dspace.to.JsonObjectToDataAddressDspaceTransformer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.client.DataPlaneSignalingClientExtension.CONTROL_CLIENT_SCOPE;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class DataPlaneSignalingClientTest {

    private static final ObjectMapper MAPPER = createObjectMapper();
    private static final int DATA_PLANE_API_PORT = getFreePort();
    private static final String DATA_PLANE_PATH = "/v1/dataflows";
    private static final String DATA_PLANE_API_URI = "http://localhost:" + DATA_PLANE_API_PORT + DATA_PLANE_PATH;
    private static final TypeTransformerRegistry TRANSFORMER_REGISTRY = new TypeTransformerRegistryImpl();
    private static final TitaniumJsonLd JSON_LD = new TitaniumJsonLd(mock(Monitor.class));
    private static final TypeManager TYPE_MANAGER = mock();

    @RegisterExtension
    static WireMockExtension dataPlane = WireMockExtension.newInstance()
            .options(wireMockConfig().port(DATA_PLANE_API_PORT))
            .build();

    private final DataPlaneInstance instance = DataPlaneInstance.Builder.newInstance().url(DATA_PLANE_API_URI).build();
    private final ControlApiHttpClient httpClient = new ControlApiHttpClientImpl(testHttpClient(), mock());

    private final DataPlaneClient dataPlaneClient = new DataPlaneSignalingClient(httpClient, TRANSFORMER_REGISTRY,
            JSON_LD, CONTROL_CLIENT_SCOPE, TYPE_MANAGER, "test", instance);

    @BeforeAll
    public static void setUp() {
        var factory = Json.createBuilderFactory(Map.of());

        JSON_LD.registerNamespace(VOCAB, EDC_NAMESPACE);
        JSON_LD.registerNamespace(VOCAB, EDC_NAMESPACE, CONTROL_CLIENT_SCOPE);
        JSON_LD.registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, CONTROL_CLIENT_SCOPE);
        JSON_LD.registerNamespace(DSPACE_PREFIX, DSPACE_SCHEMA, CONTROL_CLIENT_SCOPE);

        TRANSFORMER_REGISTRY.register(new JsonObjectFromDataFlowTerminateMessageTransformer(factory));
        TRANSFORMER_REGISTRY.register(new JsonObjectFromDataFlowSuspendMessageTransformer(factory));
        TRANSFORMER_REGISTRY.register(new JsonObjectFromDataFlowStartMessageTransformer(factory, TYPE_MANAGER, "test"));
        TRANSFORMER_REGISTRY.register(new JsonObjectFromDataFlowProvisionMessageTransformer(factory, TYPE_MANAGER, "test"));
        TRANSFORMER_REGISTRY.register(new JsonObjectFromDataFlowResponseMessageTransformer(factory));
        TRANSFORMER_REGISTRY.register(new JsonObjectToDataFlowResponseMessageTransformer());
        TRANSFORMER_REGISTRY.register(new JsonObjectFromDataAddressDspaceTransformer(factory, TYPE_MANAGER, "test"));
        TRANSFORMER_REGISTRY.register(new JsonObjectToDataAddressDspaceTransformer());
        when(TYPE_MANAGER.getMapper("test")).thenReturn(MAPPER);
    }


    private static Result<JsonObject> compact(JsonObject input) {
        return JSON_LD.compact(input, CONTROL_CLIENT_SCOPE);
    }

    @Nested
    class Start {

        @Test
        void verifyReturnFatalErrorIfReceiveResponseWithNullBody() throws JsonProcessingException {
            var flowRequest = createDataFlowRequest();

            var expected = TRANSFORMER_REGISTRY.transform(flowRequest, JsonObject.class)
                    .compose(DataPlaneSignalingClientTest::compact)
                    .orElseThrow((e) -> new EdcException(e.getFailureDetail()));

            var body = MAPPER.writeValueAsString(expected);
            dataPlane.stubFor(post(DATA_PLANE_PATH).withRequestBody(equalTo(body))
                    .willReturn(badRequest()));

            var result = dataPlaneClient.start(flowRequest);

            dataPlane.verify(postRequestedFor(urlEqualTo(DATA_PLANE_PATH)).withRequestBody(equalTo(body)));

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailure().status()).isEqualTo(FATAL_ERROR);
            assertThat(result.getFailureMessages())
                    .anySatisfy(s -> assertThat(s).contains("400").contains(flowRequest.getProcessId()));
        }

        @Test
        void verifyReturnFatalErrorIfReceiveErrorInResponse() throws JsonProcessingException {
            var flowRequest = createDataFlowRequest();

            var expected = TRANSFORMER_REGISTRY.transform(flowRequest, JsonObject.class)
                    .compose(DataPlaneSignalingClientTest::compact)
                    .orElseThrow((e) -> new EdcException(e.getFailureDetail()));

            var errorMsg = UUID.randomUUID().toString();
            var body = MAPPER.writeValueAsString(expected);
            dataPlane.stubFor(post(DATA_PLANE_PATH).withRequestBody(equalTo(body))
                    .willReturn(aResponse().withStatus(400).withBody(errorMsg)));

            var result = dataPlaneClient.start(flowRequest);

            dataPlane.verify(postRequestedFor(urlEqualTo(DATA_PLANE_PATH)).withRequestBody(equalTo(body)));

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailure().status()).isEqualTo(FATAL_ERROR);
            assertThat(result.getFailureMessages()).anySatisfy(s -> assertThat(s).contains("400").contains(flowRequest.getProcessId()));
        }

        @Test
        void verifyReturnFatalErrorIfTransformFails() {
            var flowRequest = createDataFlowRequest();
            TypeTransformerRegistry registry = mock();
            var dataPlaneClient = new DataPlaneSignalingClient(httpClient, registry, JSON_LD, CONTROL_CLIENT_SCOPE, TYPE_MANAGER, "test", instance);

            when(registry.transform(any(), any())).thenReturn(Result.failure("Transform Failure"));

            var result = dataPlaneClient.start(flowRequest);

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailure().status()).isEqualTo(FATAL_ERROR);
            assertThat(result.getFailureMessages())
                    .anySatisfy(s -> assertThat(s)
                            .isEqualTo("Transform Failure")
                    );
        }

        @Test
        void verifyReturnFatalError_whenBadResponse() throws JsonProcessingException {
            var flowRequest = createDataFlowRequest();
            var expected = TRANSFORMER_REGISTRY.transform(flowRequest, JsonObject.class)
                    .compose(DataPlaneSignalingClientTest::compact)
                    .orElseThrow((e) -> new EdcException(e.getFailureDetail()));


            var body = MAPPER.writeValueAsString(expected);
            dataPlane.stubFor(post(DATA_PLANE_PATH).withRequestBody(equalTo(body))
                    .willReturn(okJson("{}")));

            var result = dataPlaneClient.start(flowRequest);

            dataPlane.verify(postRequestedFor(urlEqualTo(DATA_PLANE_PATH)).withRequestBody(equalTo(body)));

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailure().status()).isEqualTo(FATAL_ERROR);
            assertThat(result.getFailureMessages())
                    .anySatisfy(s -> assertThat(s)
                            .contains("Error expanding JSON-LD structure")
                    );
        }

        @Test
        void verifyTransferSuccess() throws JsonProcessingException {
            var flowRequest = createDataFlowRequest();
            var expected = TRANSFORMER_REGISTRY.transform(flowRequest, JsonObject.class)
                    .compose(DataPlaneSignalingClientTest::compact)
                    .orElseThrow((e) -> new EdcException(e.getFailureDetail()));

            var flowResponse = DataFlowResponseMessage.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("type").build()).build();
            var response = TRANSFORMER_REGISTRY.transform(flowResponse, JsonObject.class)
                    .compose(JSON_LD::compact)
                    .orElseThrow((e) -> new EdcException(e.getFailureDetail()));


            var body = MAPPER.writeValueAsString(expected);
            dataPlane.stubFor(post(DATA_PLANE_PATH).withRequestBody(equalTo(body))
                    .willReturn(okJson(MAPPER.writeValueAsString(response))));

            var result = dataPlaneClient.start(flowRequest);

            dataPlane.verify(postRequestedFor(urlEqualTo(DATA_PLANE_PATH)).withRequestBody(equalTo(body)));

            assertThat(result).isSucceeded().extracting(DataFlowResponseMessage::getDataAddress).isNotNull();
        }

        @Test
        void verifyTransferSuccess_withoutDataAddress() throws JsonProcessingException {
            var flowRequest = createDataFlowRequest();
            var expected = TRANSFORMER_REGISTRY.transform(flowRequest, JsonObject.class)
                    .compose(DataPlaneSignalingClientTest::compact)
                    .orElseThrow((e) -> new EdcException(e.getFailureDetail()));

            var flowResponse = DataFlowResponseMessage.Builder.newInstance().build();
            var response = TRANSFORMER_REGISTRY.transform(flowResponse, JsonObject.class)
                    .compose(JSON_LD::compact)
                    .orElseThrow((e) -> new EdcException(e.getFailureDetail()));


            var body = MAPPER.writeValueAsString(expected);
            dataPlane.stubFor(post(DATA_PLANE_PATH).withRequestBody(equalTo(body))
                    .willReturn(okJson(MAPPER.writeValueAsString(response))));

            var result = dataPlaneClient.start(flowRequest);

            dataPlane.verify(postRequestedFor(urlEqualTo(DATA_PLANE_PATH)).withRequestBody(equalTo(body)));

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent().getDataAddress()).isNull();
        }


        private DataFlowStartMessage createDataFlowRequest() {
            return DataFlowStartMessage.Builder.newInstance()
                    .id("123")
                    .processId("456")
                    .transferType(new TransferType("DestinationType", FlowType.PULL))
                    .assetId("assetId")
                    .agreementId("agreementId")
                    .participantId("participantId")
                    .callbackAddress(URI.create("http://void"))
                    .sourceDataAddress(DataAddress.Builder.newInstance().type("test").build())
                    .destinationDataAddress(DataAddress.Builder.newInstance().type("test").build())
                    .build();
        }

    }

    @Nested
    class Provision {

        @Test
        void shouldReturnFatalError_whenReceiveResponseWithNullBody() throws JsonProcessingException {
            var request = createProvisionRequest();

            var expected = TRANSFORMER_REGISTRY.transform(request, JsonObject.class)
                    .compose(DataPlaneSignalingClientTest::compact)
                    .orElseThrow((e) -> new EdcException(e.getFailureDetail()));

            var body = MAPPER.writeValueAsString(expected);
            dataPlane.stubFor(post(DATA_PLANE_PATH).withRequestBody(equalTo(body))
                    .willReturn(badRequest()));


            var result = dataPlaneClient.provision(request);

            dataPlane.verify(postRequestedFor(urlEqualTo(DATA_PLANE_PATH)).withRequestBody(equalTo(body)));

            assertThat(result).isFailed().extracting(ResponseFailure::status).isEqualTo(FATAL_ERROR);
            assertThat(result.getFailureMessages())
                    .anySatisfy(s -> assertThat(s).contains("400").contains(request.getProcessId()));
        }

        @Test
        void verifyReturnFatalErrorIfReceiveErrorInResponse() throws JsonProcessingException {
            var request = createProvisionRequest();

            var expected = TRANSFORMER_REGISTRY.transform(request, JsonObject.class)
                    .compose(DataPlaneSignalingClientTest::compact)
                    .orElseThrow((e) -> new EdcException(e.getFailureDetail()));

            var errorMsg = UUID.randomUUID().toString();

            var body = MAPPER.writeValueAsString(expected);
            dataPlane.stubFor(post(DATA_PLANE_PATH).withRequestBody(equalTo(body))
                    .willReturn(aResponse().withStatus(400).withBody(errorMsg)));

            var result = dataPlaneClient.provision(request);

            dataPlane.verify(postRequestedFor(urlEqualTo(DATA_PLANE_PATH)).withRequestBody(equalTo(body)));

            assertThat(result).isFailed().extracting(ResponseFailure::status).isEqualTo(FATAL_ERROR);
            assertThat(result.getFailureMessages()).anySatisfy(s -> assertThat(s).contains("400").contains(request.getProcessId()));
        }

        @Test
        void verifyReturnFatalErrorIfTransformFails() {
            var request = createProvisionRequest();
            TypeTransformerRegistry registry = mock();
            var dataPlaneClient = new DataPlaneSignalingClient(httpClient, registry, JSON_LD, CONTROL_CLIENT_SCOPE, TYPE_MANAGER, "test", instance);

            when(registry.transform(any(), any())).thenReturn(Result.failure("Transform Failure"));

            var result = dataPlaneClient.provision(request);

            assertThat(result).isFailed().extracting(ResponseFailure::status).isEqualTo(FATAL_ERROR);
            assertThat(result.getFailureMessages())
                    .anySatisfy(s -> assertThat(s)
                            .isEqualTo("Transform Failure")
                    );
        }

        @Test
        void verifyReturnFatalError_whenBadResponse() throws JsonProcessingException {
            var request = createProvisionRequest();
            var expected = TRANSFORMER_REGISTRY.transform(request, JsonObject.class)
                    .compose(DataPlaneSignalingClientTest::compact)
                    .orElseThrow((e) -> new EdcException(e.getFailureDetail()));


            var body = MAPPER.writeValueAsString(expected);
            dataPlane.stubFor(post(DATA_PLANE_PATH).withRequestBody(equalTo(body))
                    .willReturn(okJson("{}")));

            var result = dataPlaneClient.provision(request);

            dataPlane.verify(postRequestedFor(urlEqualTo(DATA_PLANE_PATH)).withRequestBody(equalTo(body)));

            assertThat(result).isFailed().extracting(ResponseFailure::status).isEqualTo(FATAL_ERROR);
            assertThat(result.getFailureMessages())
                    .anySatisfy(s -> assertThat(s)
                            .contains("Error expanding JSON-LD structure")
                    );
        }

        @Test
        void shouldProvision() throws JsonProcessingException {
            var request = createProvisionRequest();
            var expected = TRANSFORMER_REGISTRY.transform(request, JsonObject.class)
                    .compose(DataPlaneSignalingClientTest::compact)
                    .orElseThrow((e) -> new EdcException(e.getFailureDetail()));

            var flowResponse = DataFlowResponseMessage.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("type").build()).build();
            var response = TRANSFORMER_REGISTRY.transform(flowResponse, JsonObject.class)
                    .compose(JSON_LD::compact)
                    .orElseThrow((e) -> new EdcException(e.getFailureDetail()));


            var body = MAPPER.writeValueAsString(expected);
            dataPlane.stubFor(post(DATA_PLANE_PATH).withRequestBody(equalTo(body))
                    .willReturn(okJson(MAPPER.writeValueAsString(response))));

            var result = dataPlaneClient.provision(request);

            dataPlane.verify(postRequestedFor(urlEqualTo(DATA_PLANE_PATH)).withRequestBody(equalTo(body)));

            assertThat(result).isSucceeded().extracting(DataFlowResponseMessage::getDataAddress).isNotNull();
        }

        @Test
        void shouldProvision_withoutDataAddress() throws JsonProcessingException {
            var request = createProvisionRequest();
            var expected = TRANSFORMER_REGISTRY.transform(request, JsonObject.class)
                    .compose(DataPlaneSignalingClientTest::compact)
                    .orElseThrow((e) -> new EdcException(e.getFailureDetail()));

            var flowResponse = DataFlowResponseMessage.Builder.newInstance().build();
            var response = TRANSFORMER_REGISTRY.transform(flowResponse, JsonObject.class)
                    .compose(JSON_LD::compact)
                    .orElseThrow((e) -> new EdcException(e.getFailureDetail()));


            var body = MAPPER.writeValueAsString(expected);
            dataPlane.stubFor(post(DATA_PLANE_PATH).withRequestBody(equalTo(body))
                    .willReturn(okJson(MAPPER.writeValueAsString(response))));

            var result = dataPlaneClient.provision(request);

            dataPlane.verify(postRequestedFor(urlEqualTo(DATA_PLANE_PATH)).withRequestBody(equalTo(body)));

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent().getDataAddress()).isNull();
        }


        private DataFlowProvisionMessage createProvisionRequest() {
            return DataFlowProvisionMessage.Builder.newInstance()
                    .processId("456")
                    .transferType(new TransferType("DestinationType", FlowType.PULL))
                    .assetId("assetId")
                    .agreementId("agreementId")
                    .participantId("participantId")
                    .callbackAddress(URI.create("http://void"))
                    .destination(DataAddress.Builder.newInstance().type("test").build())
                    .build();
        }

    }

    @Nested
    class Terminate {

        @Test
        void shouldCallTerminateOnAllTheAvailableDataPlanes() {

            dataPlane.stubFor(post(DATA_PLANE_PATH + "/processId/terminate")
                    .willReturn(aResponse().withStatus(204)));

            var result = dataPlaneClient.terminate("processId");

            assertThat(result).isSucceeded();
            dataPlane.verify(postRequestedFor(urlEqualTo(DATA_PLANE_PATH + "/processId/terminate")));
        }

        @Test
        void shouldFail_whenConflictResponse() {

            dataPlane.stubFor(post(DATA_PLANE_PATH + "/processId/terminate")
                    .willReturn(aResponse().withStatus(409)));


            var result = dataPlaneClient.terminate("processId");

            assertThat(result).isFailed();
        }

        @Test
        void verifyReturnFatalErrorIfTransformFails() {
            TypeTransformerRegistry registry = mock();
            var dataPlaneClient = new DataPlaneSignalingClient(httpClient, registry, JSON_LD, CONTROL_CLIENT_SCOPE, TYPE_MANAGER, "test", instance);

            when(registry.transform(any(), any())).thenReturn(Result.failure("Transform Failure"));

            var result = dataPlaneClient.terminate("processId");

            assertThat(result.failed()).isTrue();
            assertThat(result.getFailure().status()).isEqualTo(FATAL_ERROR);
            assertThat(result.getFailureMessages())
                    .anySatisfy(s -> assertThat(s)
                            .isEqualTo("Transform Failure")
                    );
        }

    }

    @Nested
    class CheckAvailability {
        @Test
        void shouldSucceed_whenDataPlaneIsAvailable() {
            dataPlane.stubFor(get(DATA_PLANE_PATH + "/check")
                    .willReturn(aResponse().withStatus(204)));

            var result = dataPlaneClient.checkAvailability();

            assertThat(result).isSucceeded();
        }

        @Test
        void shouldFail_whenDataPlaneIsNotAvailable() {

            dataPlane.stubFor(WireMock.get(anyUrl())
                    .willReturn(aResponse().withStatus(404)));

            var result = dataPlaneClient.checkAvailability();

            assertThat(result).isFailed();
        }
    }
}
