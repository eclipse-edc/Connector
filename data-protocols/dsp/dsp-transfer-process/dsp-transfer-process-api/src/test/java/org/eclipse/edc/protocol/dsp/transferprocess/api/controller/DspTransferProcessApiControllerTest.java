/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.api.controller;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.protocol.dsp.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.spi.message.MessageSpecHandler;
import org.eclipse.edc.protocol.dsp.spi.message.PostDspRequest;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static jakarta.json.Json.createObjectBuilder;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_COMPLETION;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_INITIAL_REQUEST;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_START;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_SUSPENSION;
import static org.eclipse.edc.protocol.dsp.transferprocess.api.TransferProcessApiPaths.TRANSFER_TERMINATION;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_ERROR;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//@ApiTest
class DspTransferProcessApiControllerTest extends RestControllerTestBase {

    private static final String PROCESS_ID = "testId";
    private final IdentityService identityService = mock();
    private final TypeTransformerRegistry registry = mock();
    private final TransferProcessProtocolService protocolService = mock();
    private final MessageSpecHandler messageSpecHandler = mock();
    private final String callbackAddress = "http://callback";

    private static JsonObject transferRequestJson() {
        return Json.createObjectBuilder().add(TYPE, DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE).build();
    }

    private static JsonObject transferStartJson() {
        return Json.createObjectBuilder()
                .add(TYPE, DSPACE_TYPE_TRANSFER_START_MESSAGE)
                .build();
    }

    private static JsonObject transferCompletionJson() {
        return Json.createObjectBuilder()
                .add(TYPE, DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE)
                .build();
    }

    private static JsonObject transferTerminationJson() {
        return Json.createObjectBuilder()
                .add(TYPE, DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE)
                .build();
    }

    private static TransferRequestMessage transferRequestMessage() {
        return TransferRequestMessage.Builder.newInstance()
                .protocol("protocol")
                .processId(PROCESS_ID)
                .callbackAddress("http://connector")
                .counterPartyAddress("http://connector")
                .build();
    }

    private static TransferStartMessage transferStartMessage() {
        return TransferStartMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://connector")
                .processId(PROCESS_ID)
                .build();
    }

    private static TransferCompletionMessage transferCompletionMessage() {
        return TransferCompletionMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://connector")
                .processId(PROCESS_ID)
                .build();
    }

    private static TransferTerminationMessage transferTerminationMessage() {
        return TransferTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://connector")
                .processId(PROCESS_ID)
                .build();
    }
    
    @Test
    void getTransferProcess_shouldGetResource() {
        var id = "transferProcessId";

        when(messageSpecHandler.getResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());
    
        baseRequest()
                .get(BASE_PATH + id)
                .then()
                .contentType(MediaType.APPLICATION_JSON)
                .statusCode(200);
    
        var captor = ArgumentCaptor.forClass(GetDspRequest.class);
        verify(messageSpecHandler).getResource(captor.capture());
        var dspRequest = captor.getValue();
        assertThat(dspRequest.getId()).isEqualTo("transferProcessId");
        assertThat(dspRequest.getResultClass()).isEqualTo(TransferProcess.class);
        assertThat(dspRequest.getToken()).isEqualTo("auth");
        assertThat(dspRequest.getErrorType()).isEqualTo(DSPACE_TYPE_TRANSFER_ERROR);
    }

    @Test
    void initiateTransferProcess_shouldCreateResource() {
        var token = token();
        var message = transferRequestMessage();
        var process = transferProcess();
        var json = Json.createObjectBuilder().build();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress))).thenReturn(Result.success(token));
        when(registry.transform(any(JsonObject.class), eq(TransferRequestMessage.class))).thenReturn(Result.success(message));
        when(protocolService.notifyRequested(message, token)).thenReturn(ServiceResult.success(process));
        when(registry.transform(any(TransferProcess.class), eq(JsonObject.class))).thenReturn(Result.success(json));
        when(messageSpecHandler.createResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());

        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(transferRequestJson())
                .post(BASE_PATH + TRANSFER_INITIAL_REQUEST)
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON);

        assertThat(result).isNotNull();
        var captor = ArgumentCaptor.forClass(PostDspRequest.class);
        verify(messageSpecHandler).createResource(captor.capture());
        var request = captor.getValue();
        assertThat(request.getToken()).isEqualTo("auth");
        assertThat(request.getProcessId()).isEqualTo(null);
        assertThat(request.getInputClass()).isEqualTo(TransferRequestMessage.class);
        assertThat(request.getResultClass()).isEqualTo(TransferProcess.class);
        assertThat(request.getMessage()).isNotNull();
        assertThat(request.getExpectedMessageType()).isEqualTo(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE);
        assertThat(request.getErrorType()).isEqualTo(DSPACE_TYPE_TRANSFER_ERROR);
    }

    @Test
    void consumerTransferProcessSuspension_shouldReturnNotImplemented_whenOperationNotSupported() {
        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder().build())
                .post(BASE_PATH + PROCESS_ID + TRANSFER_SUSPENSION)
                .then()
                .statusCode(501)
                .extract().as(JsonObject.class);

        assertThat(result).isNotNull();
        assertThat(result.getString(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_TYPE_TRANSFER_ERROR);
        assertThat(result.getString(DSPACE_PROPERTY_CODE)).isEqualTo("501");
        assertThat(result.get(DSPACE_PROPERTY_PROCESS_ID)).isNotNull();
        assertThat(result.get(DSPACE_PROPERTY_REASON)).isNotNull();
    }

    /**
     * Verifies that an endpoint returns 401 if the identity service cannot verify the identity token.
     *
     * @param path the request path to the endpoint
     */
    @ParameterizedTest
    @ArgumentsSource(ControllerMethodArguments.class)
    void callEndpoint_shouldUpdateResource(String path, Class<?> messageClass, String messageType) {
        when(messageSpecHandler.updateResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());
        var requestBody = createObjectBuilder().add("http://schema/key", "value").build();

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .post(path)
                .then()
                .contentType(MediaType.APPLICATION_JSON)
                .statusCode(200);

        var captor = ArgumentCaptor.forClass(PostDspRequest.class);
        verify(messageSpecHandler).updateResource(captor.capture());
        var request = captor.getValue();
        assertThat(request.getExpectedMessageType());
        assertThat(request.getToken()).isEqualTo("auth");
        assertThat(request.getProcessId()).isEqualTo(PROCESS_ID);
        assertThat(request.getMessage()).isNotNull();
        assertThat(request.getInputClass()).isEqualTo(messageClass);
        assertThat(request.getResultClass()).isEqualTo(TransferProcess.class);
        assertThat(request.getExpectedMessageType()).isEqualTo(messageType);
    }

    @Override
    protected Object controller() {
        return new DspTransferProcessApiController(protocolService, messageSpecHandler);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/")
                .header(HttpHeaders.AUTHORIZATION, "auth")
                .when();
    }

    private TransferProcess transferProcess() {
        return TransferProcess.Builder.newInstance()
                .id("id")
                .dataRequest(DataRequest.Builder.newInstance()
                        .dataDestination(DataAddress.Builder.newInstance().type("any").build())
                        .build())
                .build();
    }

    private static class ControllerMethodArguments implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(
                            BASE_PATH + PROCESS_ID + TRANSFER_START,
                            TransferStartMessage.class,
                            DSPACE_TYPE_TRANSFER_START_MESSAGE),
                    Arguments.of(
                            BASE_PATH + PROCESS_ID + TRANSFER_COMPLETION,
                            TransferCompletionMessage.class,
                            DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE),
                    Arguments.of(
                            BASE_PATH + PROCESS_ID + TRANSFER_TERMINATION,
                            TransferTerminationMessage.class,
                            DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE)
            );
        }
    }

    private ClaimToken token() {
        return ClaimToken.Builder.newInstance().build();
    }
}
