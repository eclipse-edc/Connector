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

package org.eclipse.edc.protocol.dsp.api.configuration.message;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.protocol.dsp.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.spi.message.PostDspRequest;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DspRequestHandlerImplTest {

    private final String callbackAddress = "http://any";
    private final IdentityService identityService = mock();
    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();
    private final DspRequestHandlerImpl handler = new DspRequestHandlerImpl(mock(), callbackAddress, identityService,
            validatorRegistry, transformerRegistry);

    @Nested
    class GetResource {

        @Test
        void shouldSucceed() {
            var claimToken = claimToken();
            var content = new Object();
            var resourceJson = Json.createObjectBuilder().build();
            BiFunction<String, ClaimToken, ServiceResult<Object>> serviceCall = (m, t) -> ServiceResult.success(content);
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken));
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(resourceJson));
            var request = GetDspRequest.Builder.newInstance(Object.class)
                    .token("token")
                    .id("id")
                    .serviceCall(serviceCall)
                    .errorType("errorType")
                    .build();

            var result = handler.getResource(request);

            assertThat(result.getStatus()).isEqualTo(200);
            verify(identityService).verifyJwtToken(argThat(t -> t.getToken().equals("token")), eq(callbackAddress));
            verifyNoInteractions(validatorRegistry);
        }

        @Test
        void shouldFail_whenTokenIsNotValid() {
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.failure("error"));
            var request = getDspRequestBuilder().errorType("errorType").build();

            var result = handler.getResource(request);

            assertThat(result.getStatus()).isEqualTo(401);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("errorType");
                assertThat(error.getString(DSPACE_PROPERTY_CODE)).isEqualTo("401");
                assertThat(error.get(DSPACE_PROPERTY_REASON)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenServiceCallFails() {
            var claimToken = claimToken();
            BiFunction<String, ClaimToken, ServiceResult<Object>> serviceCall = (m, t) -> ServiceResult.notFound("error");
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken));
            var request = getDspRequestBuilder()
                    .serviceCall(serviceCall)
                    .build();

            var result = handler.getResource(request);

            assertThat(result.getStatus()).isEqualTo(404);
        }

        @Test
        void shouldFail_whenTransformationFails() {
            var claimToken = claimToken();
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken));
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));
            var request = getDspRequestBuilder().build();

            var result = handler.getResource(request);

            assertThat(result.getStatus()).isEqualTo(500);
        }

        private GetDspRequest.Builder<Object> getDspRequestBuilder() {
            return GetDspRequest.Builder.newInstance(Object.class)
                    .token("token")
                    .id("id")
                    .serviceCall((i, c) -> ServiceResult.success())
                    .errorType("errorType");
        }
    }

    @Nested
    class CreateResource {
        @Test
        void shouldSucceed() {
            var claimToken = claimToken();
            var jsonMessage = Json.createObjectBuilder().build();
            var message = mock(TestMessage.class);
            var content = new Object();
            var responseJson = Json.createObjectBuilder().build();
            BiFunction<TestMessage, ClaimToken, ServiceResult<Object>> serviceCall = (m, t) -> ServiceResult.success(content);
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(TestMessage.class))).thenReturn(Result.success(message));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(responseJson));
            var request = PostDspRequest.Builder.newInstance(TestMessage.class, Object.class)
                    .token("token")
                    .expectedMessageType("expected-message-type")
                    .message(jsonMessage)
                    .serviceCall(serviceCall)
                    .errorType("errorType")
                    .build();

            var result = handler.createResource(request);

            assertThat(result.getStatus()).isEqualTo(200);
            assertThat(result.getEntity()).isEqualTo(responseJson);
            assertThat(result.getMediaType()).isEqualTo(APPLICATION_JSON_TYPE);
            verify(identityService).verifyJwtToken(argThat(t -> t.getToken().equals("token")), eq(callbackAddress));
            verify(validatorRegistry).validate("expected-message-type", jsonMessage);
            verify(transformerRegistry).transform(jsonMessage, TestMessage.class);
            verify(message).setProtocol(DATASPACE_PROTOCOL_HTTP);
            verify(transformerRegistry).transform(content, JsonObject.class);
        }

        @Test
        void shouldFail_whenTokenIsNotValid() {
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.failure("error"));
            var request = postDspRequestBuilder().errorType("errorType").build();

            var result = handler.createResource(request);

            assertThat(result.getStatus()).isEqualTo(401);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("errorType");
                assertThat(error.getString(DSPACE_PROPERTY_CODE)).isEqualTo("401");
                assertThat(error.get(DSPACE_PROPERTY_REASON)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenValidationFails() {
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken()));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("error", "path")));
            var request = postDspRequestBuilder().build();

            var result = handler.createResource(request);

            assertThat(result.getStatus()).isEqualTo(400);
        }

        @Test
        void shouldFail_whenTransformationFails() {
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken()));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));
            var request = postDspRequestBuilder().build();

            var result = handler.createResource(request);

            assertThat(result.getStatus()).isEqualTo(400);
        }

        @Test
        void shouldFail_whenServiceCallFails() {
            var claimToken = claimToken();
            var message = mock(TestMessage.class);
            BiFunction<TestMessage, ClaimToken, ServiceResult<Object>> serviceCall = (m, t) -> ServiceResult.conflict("error");
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(message));
            var request = postDspRequestBuilder().serviceCall(serviceCall).build();

            var result = handler.createResource(request);

            assertThat(result.getStatus()).isEqualTo(409);
        }

        @Test
        void shouldReturnInternalServerError_whenOutputTransformationFails() {
            var claimToken = claimToken();
            var message = mock(TestMessage.class);
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(TestMessage.class))).thenReturn(Result.success(message));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.failure("error"));
            var request = postDspRequestBuilder().build();

            var result = handler.createResource(request);

            assertThat(result.getStatus()).isEqualTo(500);
        }

        private PostDspRequest.Builder<TestMessage, Object> postDspRequestBuilder() {
            return PostDspRequest.Builder
                    .newInstance(TestMessage.class, Object.class)
                    .errorType("errorType")
                    .token("token")
                    .serviceCall((i, c) -> ServiceResult.success());
        }

    }

    @Nested
    class UpdateResource {
        @Test
        void shouldSucceed() {
            var claimToken = claimToken();
            var jsonMessage = Json.createObjectBuilder().build();
            var message = mock(TestMessage.class);
            var content = new Object();
            BiFunction<TestMessage, ClaimToken, ServiceResult<Object>> serviceCall = (m, t) -> ServiceResult.success(content);
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(TestMessage.class))).thenReturn(Result.success(message));
            var request = PostDspRequest.Builder.newInstance(TestMessage.class, Object.class)
                    .token("token")
                    .expectedMessageType("expected-message-type")
                    .message(jsonMessage)
                    .serviceCall(serviceCall)
                    .errorType("errorType")
                    .build();

            var result = handler.updateResource(request);

            assertThat(result.getStatus()).isEqualTo(200);
            assertThat(result.getMediaType()).isEqualTo(APPLICATION_JSON_TYPE);
            verify(identityService).verifyJwtToken(argThat(t -> t.getToken().equals("token")), eq(callbackAddress));
            verify(validatorRegistry).validate("expected-message-type", jsonMessage);
            verify(transformerRegistry).transform(jsonMessage, TestMessage.class);
            verify(message).setProtocol(DATASPACE_PROTOCOL_HTTP);
        }

        @Test
        void shouldFail_whenTokenIsNotValid() {
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.failure("error"));
            var request = postDspRequestBuilder().processId("processId").errorType("errorType").build();

            var result = handler.updateResource(request);

            assertThat(result.getStatus()).isEqualTo(401);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("errorType");
                assertThat(error.getString(DSPACE_PROPERTY_CODE)).isEqualTo("401");
                assertThat(error.getString(DSPACE_PROPERTY_PROCESS_ID)).isEqualTo("processId");
                assertThat(error.get(DSPACE_PROPERTY_REASON)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenValidationFails() {
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken()));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("error", "path")));
            var request = postDspRequestBuilder().processId("processId").errorType("errorType").build();

            var result = handler.updateResource(request);

            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("errorType");
                assertThat(error.getString(DSPACE_PROPERTY_CODE)).isEqualTo("400");
                assertThat(error.getString(DSPACE_PROPERTY_PROCESS_ID)).isEqualTo("processId");
                assertThat(error.get(DSPACE_PROPERTY_REASON)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenTransformationFails() {
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken()));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));
            var request = postDspRequestBuilder().processId("processId").errorType("errorType").build();

            var result = handler.updateResource(request);

            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("errorType");
                assertThat(error.getString(DSPACE_PROPERTY_CODE)).isEqualTo("400");
                assertThat(error.getString(DSPACE_PROPERTY_PROCESS_ID)).isEqualTo("processId");
                assertThat(error.get(DSPACE_PROPERTY_REASON)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenIdIsNotValid() {
            var claimToken = claimToken();
            var message = mock(TestMessage.class);
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(message));
            when(message.getProcessId()).thenReturn("processId");
            var request = postDspRequestBuilder().processId("differentId").errorType("errorType").build();

            var result = handler.updateResource(request);

            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("errorType");
                assertThat(error.getString(DSPACE_PROPERTY_CODE)).isEqualTo("400");
                assertThat(error.getString(DSPACE_PROPERTY_PROCESS_ID)).isEqualTo("differentId");
                assertThat(error.get(DSPACE_PROPERTY_REASON)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenServiceCallFails() {
            var claimToken = claimToken();
            var message = mock(TestMessage.class);
            BiFunction<TestMessage, ClaimToken, ServiceResult<Object>> serviceCall = (m, t) -> ServiceResult.conflict("error");
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(message));
            when(message.getProcessId()).thenReturn("processId");
            var request = postDspRequestBuilder().processId("processId").serviceCall(serviceCall).build();

            var result = handler.updateResource(request);

            assertThat(result.getStatus()).isEqualTo(409);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("errorType");
                assertThat(error.getString(DSPACE_PROPERTY_CODE)).isEqualTo("409");
                assertThat(error.getString(DSPACE_PROPERTY_PROCESS_ID)).isEqualTo("processId");
                assertThat(error.get(DSPACE_PROPERTY_REASON)).isNotNull();
            });
        }

        private PostDspRequest.Builder<TestMessage, Object> postDspRequestBuilder() {
            return PostDspRequest.Builder
                    .newInstance(TestMessage.class, Object.class)
                    .errorType("errorType")
                    .token("token")
                    .serviceCall((i, c) -> ServiceResult.success());
        }

    }

    private ClaimToken claimToken() {
        return ClaimToken.Builder.newInstance().build();
    }

    private static class TestMessage implements ProcessRemoteMessage {

        @Override
        public @NotNull String getId() {
            return null;
        }

        @Override
        public @NotNull String getProcessId() {
            return null;
        }

        @Override
        public void setProtocol(String protocol) {

        }

        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public String getCounterPartyAddress() {
            return null;
        }
    }

}
