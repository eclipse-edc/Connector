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
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRemoteMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
class DspTransferProcessApiControllerTest extends RestControllerTestBase {

    private static final String PROCESS_ID = "testId";
    private final IdentityService identityService = mock(IdentityService.class);
    private final TypeTransformerRegistry registry = mock(TypeTransformerRegistry.class);
    private final TransferProcessProtocolService protocolService = mock(TransferProcessProtocolService.class);
    private final String callbackAddress = "http://callback";
    private final String authHeader = "auth";

    private static ClaimToken token() {
        return ClaimToken.Builder.newInstance().build();
    }

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
    void getTransferProcess_shouldReturnNotImplemented_whenOperationNotSupported() {
        //operation not yet supported
        var result = baseRequest()
                .get(BASE_PATH + PROCESS_ID)
                .then()
                .statusCode(501)
                .extract().as(JsonObject.class);

        assertThat(result).isNotNull();
        assertThat(result.getString(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_TYPE_TRANSFER_ERROR);
        assertThat(result.getString(DSPACE_PROPERTY_CODE)).isEqualTo("501");
        assertThat(result.get(DSPACE_PROPERTY_PROCESS_ID)).isNotNull();
        assertThat(result.get(DSPACE_PROPERTY_REASON)).isNotNull();
    }

    @Test
    void initiateTransferProcess_shouldReturnTransferProcess_whenValidRequest() {
        var token = token();
        var message = transferRequestMessage();
        var process = transferProcess();
        var json = Json.createObjectBuilder().build();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress))).thenReturn(Result.success(token));
        when(registry.transform(any(JsonObject.class), eq(TransferRequestMessage.class))).thenReturn(Result.success(message));
        when(protocolService.notifyRequested(message, token)).thenReturn(ServiceResult.success(process));
        when(registry.transform(any(TransferProcess.class), eq(JsonObject.class))).thenReturn(Result.success(json));

        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(transferRequestJson())
                .post(BASE_PATH + TRANSFER_INITIAL_REQUEST)
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .extract().as(JsonObject.class);

        assertThat(result).isNotNull();
        verify(protocolService, times(1)).notifyRequested(message, token);
    }

    @Test
    void initiateTransferProcess_shouldReturnInternalServerError_whenTransferProcessTransformationFails() {
        var token = token();
        var message = transferRequestMessage();
        var process = transferProcess();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.success(token));
        when(registry.transform(any(JsonObject.class), eq(TransferRequestMessage.class)))
                .thenReturn(Result.success(message));
        when(protocolService.notifyRequested(message, token)).thenReturn(ServiceResult.success(process));
        when(registry.transform(any(TransferProcess.class), eq(JsonObject.class))).thenReturn(Result.failure("error"));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(transferRequestJson())
                .post(BASE_PATH + TRANSFER_INITIAL_REQUEST)
                .then()
                .statusCode(500);
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
    void callEndpoint_shouldReturnUnauthorized_whenNotAuthorized(String path, JsonObject request) {
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.failure("error"));

        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(path)
                .then()
                .contentType(MediaType.APPLICATION_JSON)
                .statusCode(401)
                .extract().as(JsonObject.class);

        assertThat(result).isNotNull();
        assertThat(result.getString(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_TYPE_TRANSFER_ERROR);
        assertThat(result.getString(DSPACE_PROPERTY_CODE)).isEqualTo("401");
        assertThat(result.get(DSPACE_PROPERTY_REASON)).isNotNull();

        if (!path.equals(BASE_PATH + TRANSFER_INITIAL_REQUEST)) {
            assertThat(result.get(DSPACE_PROPERTY_PROCESS_ID)).isNotNull();
        }
    }

    /**
     * Verifies that an endpoint returns 400 if the incoming message cannot be transformed.
     *
     * @param path    the request path to the endpoint
     * @param request the request body
     */
    @ParameterizedTest
    @ArgumentsSource(ControllerMethodArguments.class)
    void callEndpoint_shouldReturnBadRequest_whenRequestTransformationFails(String path, JsonObject request) {
        var token = token();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.success(token));
        when(registry.transform(any(JsonObject.class), argThat(TransferRemoteMessage.class::isAssignableFrom)))
                .thenReturn(Result.failure("error"));

        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(path)
                .then()
                .statusCode(400)
                .extract().as(JsonObject.class);

        assertThat(result).isNotNull();
        assertThat(result.getString(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_TYPE_TRANSFER_ERROR);
        assertThat(result.getString(DSPACE_PROPERTY_CODE)).isEqualTo("400");
        assertThat(result.get(DSPACE_PROPERTY_REASON)).isNotNull();

        if (!path.equals(BASE_PATH + TRANSFER_INITIAL_REQUEST)) {
            assertThat(result.get(DSPACE_PROPERTY_PROCESS_ID)).isNotNull();
        }
    }

    /**
     * Verifies that an endpoint returns 400 if the ID in the request does not match the ID in
     * the request path.
     *
     * @param path    the request path to the endpoint
     * @param request the request body
     * @param message the request message to be returned by the transformer registry
     */
    @ParameterizedTest
    @ArgumentsSource(ControllerMethodArgumentsForIdValidationFails.class)
    void callEndpoint_shouldReturnBadRequest_whenIdValidationFails(String path, JsonObject request, TransferRemoteMessage message) {
        var token = token();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.success(token));
        when(registry.transform(any(JsonObject.class), argThat(TransferRemoteMessage.class::isAssignableFrom)))
                .thenReturn(Result.success(message));

        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(path)
                .then()
                .statusCode(400)
                .extract().as(JsonObject.class);

        assertThat(result).isNotNull();
        assertThat(result.getString(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_TYPE_TRANSFER_ERROR);
        assertThat(result.getString(DSPACE_PROPERTY_CODE)).isEqualTo("400");
        assertThat(result.get(DSPACE_PROPERTY_PROCESS_ID)).isNotNull();
        assertThat(result.get(DSPACE_PROPERTY_REASON)).isNotNull();

        // verify that the message protocol was set to the DSP protocol by the controller
        assertThat(message.getProtocol()).isEqualTo(DATASPACE_PROTOCOL_HTTP);
    }

    /**
     * Verifies that an endpoint returns 204 if the call to the service was successful. Also verifies
     * that the correct service method was called. This is only applicable for endpoints that do not
     * return a response body.
     *
     * @param path          the request path to the endpoint
     * @param request       the request body
     * @param message       the request message to be returned by the transformer registry
     * @param serviceMethod reference to the service method that should be called, required to verify that it was called
     */
    @ParameterizedTest
    @ArgumentsSource(ControllerMethodArgumentsForServiceCall.class)
    void callEndpoint_shouldCallService_whenValidRequest(String path, JsonObject request, TransferRemoteMessage message, Method serviceMethod) throws Exception {
        var token = token();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress)))
                .thenReturn(Result.success(token));
        when(registry.transform(any(JsonObject.class), argThat(TransferRemoteMessage.class::isAssignableFrom)))
                .thenReturn(Result.success(message));

        when(protocolService.notifyStarted(any(TransferStartMessage.class), eq(token))).thenReturn(ServiceResult.success(transferProcess()));
        when(protocolService.notifyCompleted(any(TransferCompletionMessage.class), eq(token))).thenReturn(ServiceResult.success(transferProcess()));
        when(protocolService.notifyTerminated(any(TransferTerminationMessage.class), eq(token))).thenReturn(ServiceResult.success(transferProcess()));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(path)
                .then()
                .statusCode(200);

        // verify that the message protocol was set to the DSP protocol by the controller
        assertThat(message.getProtocol()).isEqualTo(DATASPACE_PROTOCOL_HTTP);

        var verify = verify(protocolService, times(1));
        serviceMethod.invoke(verify, message, token);
    }

    /**
     * Verifies that an endpoint returns 500 if there is an error in the service. Also verifies
     * that the correct service method was called.
     *
     * @param path          the request path to the endpoint
     * @param request       the request body
     * @param message       the request message to be returned by the transformer registry
     * @param serviceMethod reference to the service method that should be called, required to verify that it was called
     */
    @ParameterizedTest
    @ArgumentsSource(ControllerMethodArgumentsForServiceError.class)
    void callEndpoint_shouldReturnConflict_whenServiceResultConflict(String path, JsonObject request, TransferRemoteMessage message, Method serviceMethod) throws Exception {
        var token = token();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress))).thenReturn(Result.success(token));
        when(registry.transform(any(JsonObject.class), argThat(TransferRemoteMessage.class::isAssignableFrom))).thenReturn(Result.success(message));

        when(protocolService.notifyRequested(any(TransferRequestMessage.class), eq(token))).thenReturn(ServiceResult.conflict("error"));
        when(protocolService.notifyStarted(any(TransferStartMessage.class), eq(token))).thenReturn(ServiceResult.conflict("error"));
        when(protocolService.notifyCompleted(any(TransferCompletionMessage.class), eq(token))).thenReturn(ServiceResult.conflict("error"));
        when(protocolService.notifyTerminated(any(TransferTerminationMessage.class), eq(token))).thenReturn(ServiceResult.conflict("error"));

        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(path)
                .then()
                .statusCode(409);

        var verify = verify(protocolService, times(1));
        serviceMethod.invoke(verify, message, token);

        assertThat(result).isNotNull();

        // verify that the message protocol was set to the DSP protocol by the controller
        assertThat(message.getProtocol()).isEqualTo(DATASPACE_PROTOCOL_HTTP);
    }

    @Override
    protected Object controller() {
        return new DspTransferProcessApiController(mock(Monitor.class), registry, protocolService, identityService, callbackAddress, new TitaniumJsonLd(monitor));
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .when();
    }

    private TransferProcess transferProcess() {
        return TransferProcess.Builder.newInstance().id("id").build();
    }

    private static class ControllerMethodArguments implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(BASE_PATH + TRANSFER_INITIAL_REQUEST, transferRequestJson()),
                    Arguments.of(BASE_PATH + PROCESS_ID + TRANSFER_START, transferStartJson()),
                    Arguments.of(BASE_PATH + PROCESS_ID + TRANSFER_COMPLETION, transferCompletionJson()),
                    Arguments.of(BASE_PATH + PROCESS_ID + TRANSFER_TERMINATION, transferTerminationJson())
            );
        }
    }

    private static class ControllerMethodArgumentsForIdValidationFails implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(BASE_PATH + "invalidId" + TRANSFER_START, transferStartJson(), transferStartMessage()),
                    Arguments.of(BASE_PATH + "invalidId" + TRANSFER_COMPLETION, transferCompletionJson(), transferCompletionMessage()),
                    Arguments.of(BASE_PATH + "invalidId" + TRANSFER_TERMINATION, transferTerminationJson(), transferTerminationMessage())
            );
        }
    }

    private static class ControllerMethodArgumentsForServiceCall implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(BASE_PATH + PROCESS_ID + TRANSFER_START, transferStartJson(), transferStartMessage(),
                            TransferProcessProtocolService.class.getDeclaredMethod("notifyStarted", TransferStartMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + PROCESS_ID + TRANSFER_COMPLETION, transferCompletionJson(), transferCompletionMessage(),
                            TransferProcessProtocolService.class.getDeclaredMethod("notifyCompleted", TransferCompletionMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + PROCESS_ID + TRANSFER_TERMINATION, transferTerminationJson(), transferTerminationMessage(),
                            TransferProcessProtocolService.class.getDeclaredMethod("notifyTerminated", TransferTerminationMessage.class, ClaimToken.class))
            );
        }
    }

    private static class ControllerMethodArgumentsForServiceError implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(BASE_PATH + TRANSFER_INITIAL_REQUEST, transferRequestJson(), transferRequestMessage(),
                            TransferProcessProtocolService.class.getDeclaredMethod("notifyRequested", TransferRequestMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + PROCESS_ID + TRANSFER_START, transferStartJson(), transferStartMessage(),
                            TransferProcessProtocolService.class.getDeclaredMethod("notifyStarted", TransferStartMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + PROCESS_ID + TRANSFER_COMPLETION, transferCompletionJson(), transferCompletionMessage(),
                            TransferProcessProtocolService.class.getDeclaredMethod("notifyCompleted", TransferCompletionMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + PROCESS_ID + TRANSFER_TERMINATION, transferTerminationJson(), transferTerminationMessage(),
                            TransferProcessProtocolService.class.getDeclaredMethod("notifyTerminated", TransferTerminationMessage.class, ClaimToken.class))
            );
        }
    }
}
