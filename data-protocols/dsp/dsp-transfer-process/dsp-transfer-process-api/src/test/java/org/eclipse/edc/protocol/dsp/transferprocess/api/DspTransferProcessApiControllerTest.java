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

package org.eclipse.edc.protocol.dsp.transferprocess.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.assertj.core.api.ThrowableAssert;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRemoteMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.jsonld.JsonLdExtension.TYPE_MANAGER_CONTEXT_JSON_LD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DspTransferProcessApiControllerTest {
    
    private static final ObjectMapper MAPPER = mock(ObjectMapper.class);
    private static final IdentityService IDENTITY_SERVICE = mock(IdentityService.class);
    private static final JsonLdTransformerRegistry REGISTRY = mock(JsonLdTransformerRegistry.class);
    private static final TransferProcessProtocolService PROTOCOL_SERVICE = mock(TransferProcessProtocolService.class);
    private static final String CALLBACK_ADDRESS = "http://callback";
    private static final String AUTH_HEADER = "auth";
    
    private static JsonObject request;
    
    private static DspTransferProcessApiController controller;
    
    @BeforeAll
    static void setUp() {
        var typeManager = mock(TypeManager.class);
        when(typeManager.getMapper(TYPE_MANAGER_CONTEXT_JSON_LD)).thenReturn(MAPPER);
        
        controller = new DspTransferProcessApiController(mock(Monitor.class), typeManager, REGISTRY, PROTOCOL_SERVICE, IDENTITY_SERVICE, CALLBACK_ADDRESS);
        
        request = Json.createObjectBuilder()
                .add("http://schema/key", "value")
                .build();
    }
    
    @Test
    void getTransferProcess_operationNotSupported_throwException() {
        assertThatThrownBy(() -> controller.getTransferProcess("id", AUTH_HEADER)).isInstanceOf(UnsupportedOperationException.class);
    }
    
    @Test
    void initiateTransferProcess_returnTransferProcess() {
        var token = token();
        var message = transferRequestMessage();
        var process = transferProcess();
        var json = Json.createObjectBuilder().build();
        var map = new HashMap<String, Object>();
        
        when(IDENTITY_SERVICE.verifyJwtToken(any(TokenRepresentation.class), eq(CALLBACK_ADDRESS)))
                .thenReturn(Result.success(token));
        when(REGISTRY.transform(any(JsonObject.class), eq(TransferRequestMessage.class)))
                .thenReturn(Result.success(message));
        when(PROTOCOL_SERVICE.notifyRequested(message, token)).thenReturn(ServiceResult.success(process));
        when(REGISTRY.transform(any(TransferProcess.class), eq(JsonObject.class))).thenReturn(Result.success(json));
        when(MAPPER.convertValue(any(JsonObject.class), eq(Map.class))).thenReturn(map);
        
        var result = controller.initiateTransferProcess(request, AUTH_HEADER);
        
        assertThat(result).isEqualTo(map);
        verify(PROTOCOL_SERVICE, times(1)).notifyRequested(message, token);
    }
    
    @Test
    void initiateTransferProcess_transferProcessTransformationFails_throwException() {
        var token = token();
        var message = transferRequestMessage();
        var process = transferProcess();
        
        when(IDENTITY_SERVICE.verifyJwtToken(any(TokenRepresentation.class), eq(CALLBACK_ADDRESS)))
                .thenReturn(Result.success(token));
        when(REGISTRY.transform(any(JsonObject.class), eq(TransferRequestMessage.class)))
                .thenReturn(Result.success(message));
        when(PROTOCOL_SERVICE.notifyRequested(message, token)).thenReturn(ServiceResult.success(process));
        when(REGISTRY.transform(any(TransferProcess.class), eq(JsonObject.class))).thenReturn(Result.failure("error"));
    
        assertThatThrownBy(() -> controller.initiateTransferProcess(request, AUTH_HEADER)).isInstanceOf(EdcException.class);
    }
    
    @Test
    void initiateTransferProcess_convertingResultFails_throwException() {
        var token = token();
        var message = transferRequestMessage();
        var process = transferProcess();
        var json = Json.createObjectBuilder().build();
        
        when(IDENTITY_SERVICE.verifyJwtToken(any(TokenRepresentation.class), eq(CALLBACK_ADDRESS)))
                .thenReturn(Result.success(token));
        when(REGISTRY.transform(any(JsonObject.class), eq(TransferRequestMessage.class)))
                .thenReturn(Result.success(message));
        when(PROTOCOL_SERVICE.notifyRequested(message, token)).thenReturn(ServiceResult.success(process));
        when(REGISTRY.transform(any(TransferProcess.class), eq(JsonObject.class))).thenReturn(Result.success(json));
        when(MAPPER.convertValue(any(JsonObject.class), eq(Map.class))).thenThrow(IllegalArgumentException.class);
    
        assertThatThrownBy(() -> controller.initiateTransferProcess(request, AUTH_HEADER)).isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    void consumerTransferProcessStart_startProcess() {
        var token = token();
        var message = transferStartMessage();
        
        when(IDENTITY_SERVICE.verifyJwtToken(any(TokenRepresentation.class), eq(CALLBACK_ADDRESS)))
                .thenReturn(Result.success(token));
        when(REGISTRY.transform(any(JsonObject.class), eq(TransferStartMessage.class)))
                .thenReturn(Result.success(message));
        when(PROTOCOL_SERVICE.notifyStarted(message, token)).thenReturn(ServiceResult.success(transferProcess()));
        
        controller.transferProcessStart("id", request, AUTH_HEADER);
        
        verify(PROTOCOL_SERVICE, times(1)).notifyStarted(message, token);
    }
    
    @Test
    void consumerTransferProcessCompletion_completeProcess() {
        var token = token();
        var message = transferCompletionMessage();
    
        when(IDENTITY_SERVICE.verifyJwtToken(any(TokenRepresentation.class), eq(CALLBACK_ADDRESS)))
                .thenReturn(Result.success(token));
        when(REGISTRY.transform(any(JsonObject.class), eq(TransferCompletionMessage.class)))
                .thenReturn(Result.success(message));
        when(PROTOCOL_SERVICE.notifyCompleted(message, token)).thenReturn(ServiceResult.success(transferProcess()));
    
        controller.transferProcessCompletion("id", request, AUTH_HEADER);
    
        verify(PROTOCOL_SERVICE, times(1)).notifyCompleted(message, token);
    }
    
    @Test
    void consumerTransferProcessTermination_terminateProcess() {
        var token = token();
        var message = transferTerminationMessage();
    
        when(IDENTITY_SERVICE.verifyJwtToken(any(TokenRepresentation.class), eq(CALLBACK_ADDRESS)))
                .thenReturn(Result.success(token));
        when(REGISTRY.transform(any(JsonObject.class), eq(TransferTerminationMessage.class)))
                .thenReturn(Result.success(message));
        when(PROTOCOL_SERVICE.notifyTerminated(message, token)).thenReturn(ServiceResult.success(transferProcess()));
    
        controller.transferProcessTermination("id", request, AUTH_HEADER);
    
        verify(PROTOCOL_SERVICE, times(1)).notifyTerminated(message, token);
    }
    
    @Test
    void consumerTransferProcessSuspension_operationNotSupported_throwException() {
        assertThatThrownBy(() -> controller.transferProcessSuspension("id", request, AUTH_HEADER)).isInstanceOf(UnsupportedOperationException.class);
    }
    
    @ParameterizedTest
    @MethodSource("controllerMethodArguments")
    void callEndpoint_unauthorized_throwException(ThrowableAssert.ThrowingCallable callable) {
        when(IDENTITY_SERVICE.verifyJwtToken(any(TokenRepresentation.class), eq(CALLBACK_ADDRESS)))
                .thenReturn(Result.failure("error"));
    
        assertThatThrownBy(callable).isInstanceOf(AuthenticationFailedException.class);
    }
    
    @ParameterizedTest
    @MethodSource("controllerMethodArguments")
    void callEndpoint_messageTransformationFails_throwException(ThrowableAssert.ThrowingCallable callable) {
        var token = token();
        
        when(IDENTITY_SERVICE.verifyJwtToken(any(TokenRepresentation.class), eq(CALLBACK_ADDRESS)))
                .thenReturn(Result.success(token));
        when(REGISTRY.transform(any(JsonObject.class), argThat(TransferRemoteMessage.class::isAssignableFrom)))
                .thenReturn(Result.failure("error"));
    
        assertThatThrownBy(callable).isInstanceOf(EdcException.class);
    }
    
    @ParameterizedTest
    @MethodSource("controllerMethodArgumentsForServiceError")
    void callEndpoint_errorInService_throwException(ThrowableAssert.ThrowingCallable callable, TransferRemoteMessage message) {
        var token = token();
        
        when(IDENTITY_SERVICE.verifyJwtToken(any(TokenRepresentation.class), eq(CALLBACK_ADDRESS)))
                .thenReturn(Result.success(token));
        when(REGISTRY.transform(any(JsonObject.class), argThat(TransferRemoteMessage.class::isAssignableFrom)))
                .thenReturn(Result.success(message));
    
        when(PROTOCOL_SERVICE.notifyRequested(any(TransferRequestMessage.class), eq(token))).thenReturn(ServiceResult.conflict("error"));
        when(PROTOCOL_SERVICE.notifyStarted(any(TransferStartMessage.class), eq(token))).thenReturn(ServiceResult.conflict("error"));
        when(PROTOCOL_SERVICE.notifyCompleted(any(TransferCompletionMessage.class), eq(token))).thenReturn(ServiceResult.conflict("error"));
        when(PROTOCOL_SERVICE.notifyTerminated(any(TransferTerminationMessage.class), eq(token))).thenReturn(ServiceResult.conflict("error"));
        
        assertThatThrownBy(callable).isInstanceOf(EdcException.class);
    }
    
    private static Stream<Arguments> controllerMethodArguments() {
        return Stream.of(
                Arguments.of((ThrowableAssert.ThrowingCallable) () -> controller.initiateTransferProcess(request, AUTH_HEADER)),
                Arguments.of((ThrowableAssert.ThrowingCallable) () -> controller.transferProcessStart("id", request, AUTH_HEADER)),
                Arguments.of((ThrowableAssert.ThrowingCallable) () -> controller.transferProcessCompletion("id", request, AUTH_HEADER)),
                Arguments.of((ThrowableAssert.ThrowingCallable) () -> controller.transferProcessTermination("id", request, AUTH_HEADER))
        );
    }
    
    private static Stream<Arguments> controllerMethodArgumentsForServiceError() {
        return Stream.of(
                Arguments.of((ThrowableAssert.ThrowingCallable) () -> controller.initiateTransferProcess(request, AUTH_HEADER), transferRequestMessage()),
                Arguments.of((ThrowableAssert.ThrowingCallable) () -> controller.transferProcessStart("id", request, AUTH_HEADER), transferStartMessage()),
                Arguments.of((ThrowableAssert.ThrowingCallable) () -> controller.transferProcessCompletion("id", request, AUTH_HEADER), transferCompletionMessage()),
                Arguments.of((ThrowableAssert.ThrowingCallable) () -> controller.transferProcessTermination("id", request, AUTH_HEADER), transferTerminationMessage())
        );
    }
    
    private static ClaimToken token() {
        return ClaimToken.Builder.newInstance().build();
    }
    
    private TransferProcess transferProcess() {
        return TransferProcess.Builder.newInstance().id("id").build();
    }
    
    private static TransferRequestMessage transferRequestMessage() {
        return TransferRequestMessage.Builder.newInstance()
                .protocol("protocol")
                .callbackAddress("http://connector")
                .build();
    }
    
    private static TransferStartMessage transferStartMessage() {
        return TransferStartMessage.Builder.newInstance()
                .protocol("protocol")
                .callbackAddress("http://connector")
                .processId("processId")
                .build();
    }
    
    private static TransferCompletionMessage transferCompletionMessage() {
        return TransferCompletionMessage.Builder.newInstance()
                .protocol("protocol")
                .callbackAddress("http://connector")
                .processId("processId")
                .build();
    }
    
    private static TransferTerminationMessage transferTerminationMessage() {
        return TransferTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .callbackAddress("http://connector")
                .processId("processId")
                .build();
    }
}
