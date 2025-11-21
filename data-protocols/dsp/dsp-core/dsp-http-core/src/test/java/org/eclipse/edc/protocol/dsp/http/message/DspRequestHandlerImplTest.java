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
 *       Schaeffler AG
 *
 */

package org.eclipse.edc.protocol.dsp.http.message;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.dsp.http.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.PostDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.ServiceCall;
import org.eclipse.edc.protocol.dsp.spi.transform.DspProtocolTypeTransformerRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.message.ErrorMessage;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_IRI;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DspRequestHandlerImplTest {

    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final DspProtocolTypeTransformerRegistry dspTransformerRegistry = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();

    private final DspRequestHandlerImpl handler = new DspRequestHandlerImpl(mock(), validatorRegistry, dspTransformerRegistry);
    private final String protocol = DATASPACE_PROTOCOL_HTTP;
    private final ParticipantContextSupplier participantContextSupplier = () -> ServiceResult.success(new ParticipantContext("participantContextId"));

    private static JsonObject error(String code, String reason, String processId) {
        var json = Json.createObjectBuilder()
                .add(TYPE, "TestError")
                .add(DSPACE_PROPERTY_CODE_IRI, code)
                .add(DSPACE_PROPERTY_REASON_IRI, reason);

        Optional.ofNullable(processId).ifPresent(id -> json.add(DSPACE_PROPERTY_PROCESS_ID, id));

        return json.build();
    }

    private static JsonObject error(String code, String reason) {
        return error(code, reason, null);
    }

    @BeforeEach
    void beforeEach() {
        when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
    }

    private static class TestProcessRemoteMessage extends ProcessRemoteMessage {

        @Override
        public Policy getPolicy() {
            return null;
        }

        public static class Builder extends ProcessRemoteMessage.Builder<TestProcessRemoteMessage, Builder> {

            protected Builder() {
                super(new TestProcessRemoteMessage());
            }

            public static Builder newInstance() {
                return new Builder();
            }

            @Override
            public Builder self() {
                return this;
            }
        }
    }

    private static class TestError extends ErrorMessage {


        public static final class Builder extends ErrorMessage.Builder<TestError, Builder> {
            private Builder() {
                super(new TestError());
            }

            public static Builder newInstance() {
                return new Builder();
            }

            @Override
            protected Builder self() {
                return this;
            }
        }
    }

    @Nested
    class GetResource {

        @Test
        void shouldSucceed() {
            var content = new Object();
            var message = new TestProcessRemoteMessage();
            ServiceCall<TestProcessRemoteMessage, Object> serviceCall = (ctx, m, t) -> ServiceResult.success(content);
            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(message));
            var request = GetDspRequest.Builder.newInstance(TestProcessRemoteMessage.class, Object.class, TestError.class)
                    .token("token")
                    .id("id")
                    .serviceCall(serviceCall)
                    .protocol(protocol)
                    .errorProvider(TestError.Builder::newInstance)
                    .participantContextProvider(participantContextSupplier)
                    .build();

            var result = handler.getResource(request);

            assertThat(result.getStatus()).isEqualTo(200);
            verifyNoInteractions(validatorRegistry);
        }

        @Test
        void shouldReturnUnauthorized_whenTokenIsNull() {
            var request = getDspRequestBuilder().token(null).serviceCall((ctx, m, t) -> ServiceResult.success()).build();
            var jsonError = error("401", "unauthorized");
            var message = new TestProcessRemoteMessage();

            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(message));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));

            var result = handler.getResource(request);

            assertThat(result.getStatus()).isEqualTo(401);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("TestError");
                assertThat(error.getString(DSPACE_PROPERTY_CODE_IRI)).isEqualTo("401");
                assertThat(error.get(DSPACE_PROPERTY_REASON_IRI)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenTokenIsNotValid() {
            var request = getDspRequestBuilder().serviceCall((ctx, m, t) -> ServiceResult.unauthorized("unauthorized")).build();
            var jsonError = error("401", "unauthorized");
            var message = new TestProcessRemoteMessage();

            when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(message));
            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));

            var result = handler.getResource(request);

            assertThat(result.getStatus()).isEqualTo(401);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("TestError");
                assertThat(error.getString(DSPACE_PROPERTY_CODE_IRI)).isEqualTo("401");
                assertThat(error.get(DSPACE_PROPERTY_REASON_IRI)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenServiceCallFails() {
            ServiceCall<TestProcessRemoteMessage, Object> serviceCall = (ctx, m, t) -> ServiceResult.notFound("error");
            var request = getDspRequestBuilder()
                    .serviceCall(serviceCall)
                    .build();
            var jsonError = error("404", "error");
            var message = new TestProcessRemoteMessage();

            when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(message));
            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));

            var result = handler.getResource(request);

            assertThat(result.getStatus()).isEqualTo(404);
        }

        @Test
        void shouldFail_whenTransformationFails() {
            var request = getDspRequestBuilder().build();
            var jsonError = error("500", "error");

            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));

            var result = handler.getResource(request);

            assertThat(result.getStatus()).isEqualTo(400);
        }

        @Test
        void shouldFail_whenProtocolParseFails() {
            var faultyProtocol = "faultyProtocol";
            when(dspTransformerRegistry.forProtocol(faultyProtocol)).thenReturn(Result.failure("faultyProtocol"));

            var request = getDspRequestBuilder().protocol(faultyProtocol).build();

            assertThatThrownBy(() -> handler.getResource(request)).isInstanceOf(EdcException.class);

        }

        @Test
        void shouldFail_whenParticipantContextSupplierFails() {
            var resourceJson = Json.createObjectBuilder().build();

            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(resourceJson));

            var request = getDspRequestBuilder()
                    .participantContextProvider(() -> ServiceResult.notFound("not found"))
                    .build();

            var result = handler.getResource(request);

            assertThat(result.getStatus()).isEqualTo(401);

        }

        private GetDspRequest.Builder<TestProcessRemoteMessage, Object, TestError> getDspRequestBuilder() {
            return GetDspRequest.Builder.newInstance(TestProcessRemoteMessage.class, Object.class, TestError.class)
                    .token("token")
                    .id("id")
                    .serviceCall((ctx, i, c) -> ServiceResult.success())
                    .errorProvider(TestError.Builder::newInstance)
                    .participantContextProvider(participantContextSupplier)
                    .protocol(protocol);
        }
    }

    @Nested
    class CreateResource {
        @Test
        void shouldSucceed() {
            var jsonMessage = Json.createObjectBuilder().build();
            var message = mock(TestProcessRemoteMessage.class);
            var content = new Object();
            var responseJson = Json.createObjectBuilder().build();
            ServiceCall<TestProcessRemoteMessage, Object> serviceCall = (ctx, m, t) -> ServiceResult.success(content);
            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(TestProcessRemoteMessage.class))).thenReturn(Result.success(message));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(responseJson));
            var request = PostDspRequest.Builder.newInstance(TestProcessRemoteMessage.class, Object.class, TestError.class)
                    .token("token")
                    .expectedMessageType("expected-message-type")
                    .message(jsonMessage)
                    .serviceCall(serviceCall)
                    .protocol(protocol)
                    .participantContextProvider(participantContextSupplier)
                    .errorProvider(TestError.Builder::newInstance)
                    .build();

            var result = handler.createResource(request);

            assertThat(result.getStatus()).isEqualTo(200);
            assertThat(result.getEntity()).isEqualTo(responseJson);
            assertThat(result.getMediaType()).isEqualTo(APPLICATION_JSON_TYPE);
            verify(validatorRegistry).validate("expected-message-type", jsonMessage);
            verify(transformerRegistry).transform(jsonMessage, TestProcessRemoteMessage.class);
            verify(message).setProtocol(DATASPACE_PROTOCOL_HTTP);
            verify(transformerRegistry).transform(content, JsonObject.class);
        }

        @Test
        void shouldReturnUnauthorized_whenTokenIsNull() {
            var request = postDspRequestBuilder().token(null).serviceCall((ctx, m, t) -> ServiceResult.success()).build();
            var jsonError = error("401", "unauthorized", request.getProcessId());

            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));

            var result = handler.createResource(request);

            assertThat(result.getStatus()).isEqualTo(401);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("TestError");
                assertThat(error.getString(DSPACE_PROPERTY_CODE_IRI)).isEqualTo("401");
                assertThat(error.get(DSPACE_PROPERTY_REASON_IRI)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenTokenIsNotValid() {
            var request = postDspRequestBuilder().serviceCall((ctx, m, t) -> ServiceResult.unauthorized("unauthorized")).build();
            var message = mock(TestProcessRemoteMessage.class);
            var jsonError = error("401", "Failure", request.getProcessId());


            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(TestProcessRemoteMessage.class))).thenReturn(Result.success(message));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));

            var result = handler.createResource(request);

            assertThat(result.getStatus()).isEqualTo(401);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("TestError");
                assertThat(error.getString(DSPACE_PROPERTY_CODE_IRI)).isEqualTo("401");
                assertThat(error.get(DSPACE_PROPERTY_REASON_IRI)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenValidationFails() {
            var request = postDspRequestBuilder().build();
            var jsonError = error("400", "Failure", request.getProcessId());

            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("error", "path")));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));


            var result = handler.createResource(request);

            assertThat(result.getStatus()).isEqualTo(400);
        }

        @Test
        void shouldFail_whenTransformationFails() {
            var request = postDspRequestBuilder().build();
            var jsonError = error("400", "Failure", request.getProcessId());

            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));

            var result = handler.createResource(request);

            assertThat(result.getStatus()).isEqualTo(400);
        }

        @Test
        void shouldFail_whenProtocolParseFails() {
            var faultyProtocol = "faultyProtocol";
            when(dspTransformerRegistry.forProtocol(faultyProtocol)).thenReturn(Result.failure(faultyProtocol));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            var request = postDspRequestBuilder().protocol(faultyProtocol).build();

            assertThatThrownBy(() -> handler.createResource(request)).isInstanceOf(EdcException.class);

        }

        @Test
        void shouldFail_whenParticipantContextSupplierFails() {
            var resourceJson = Json.createObjectBuilder().build();

            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(resourceJson));

            var request = postDspRequestBuilder()
                    .participantContextProvider(() -> ServiceResult.notFound("not found"))
                    .build();

            var result = handler.createResource(request);

            assertThat(result.getStatus()).isEqualTo(401);

        }

        @Test
        void shouldFail_whenServiceCallFails() {
            var message = mock(TestProcessRemoteMessage.class);
            var jsonError = error("409", "Failure", "processId");

            ServiceCall<TestProcessRemoteMessage, Object> serviceCall = (ctx, m, t) -> ServiceResult.conflict("error");
            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(message));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));
            var request = postDspRequestBuilder().serviceCall(serviceCall).build();

            var result = handler.createResource(request);

            assertThat(result.getStatus()).isEqualTo(409);
        }

        @Test
        void shouldReturnInternalServerError_whenOutputTransformationFails() {
            var message = mock(TestProcessRemoteMessage.class);
            var request = postDspRequestBuilder().build();
            var jsonError = error("500", "Failure", request.getProcessId());

            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(TestProcessRemoteMessage.class))).thenReturn(Result.success(message));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.failure("error"));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));

            var result = handler.createResource(request);

            assertThat(result.getStatus()).isEqualTo(500);
        }

        @Test
        void shouldDecorateResponse_whenDecoratorSpecified() {
            var jsonMessage = Json.createObjectBuilder().build();
            var message = mock(TestProcessRemoteMessage.class);
            var content = new Object();
            var responseJson = Json.createObjectBuilder().build();
            ServiceCall<TestProcessRemoteMessage, Object> serviceCall = (ctx, m, t) -> ServiceResult.success(content);
            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(TestProcessRemoteMessage.class))).thenReturn(Result.success(message));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(responseJson));
            var request = PostDspRequest.Builder.newInstance(TestProcessRemoteMessage.class, Object.class, TestError.class)
                    .token("token")
                    .expectedMessageType("expected-message-type")
                    .message(jsonMessage)
                    .serviceCall(serviceCall)
                    .protocol(protocol)
                    .participantContextProvider(participantContextSupplier)
                    .errorProvider(TestError.Builder::newInstance)
                    .build();

            var result = handler.createResource(request, (r, i, o) -> r.header("test", "test"));

            assertThat(result.getHeaderString("test")).isEqualTo("test");
        }

        private PostDspRequest.Builder<TestProcessRemoteMessage, Object, TestError> postDspRequestBuilder() {
            return PostDspRequest.Builder
                    .newInstance(TestProcessRemoteMessage.class, Object.class, TestError.class)
                    .token("token")
                    .protocol(protocol)
                    .errorProvider(TestError.Builder::newInstance)
                    .participantContextProvider(participantContextSupplier)
                    .serviceCall((ctx, i, c) -> ServiceResult.success());
        }

    }

    @Nested
    class UpdateResource {
        @Test
        void shouldSucceed() {
            var jsonMessage = Json.createObjectBuilder().build();
            var message = TestProcessRemoteMessage.Builder.newInstance().providerPid("providerPid").consumerPid("processId").build();
            var content = new Object();
            ServiceCall<TestProcessRemoteMessage, Object> serviceCall = (ctx, m, t) -> ServiceResult.success(content);
            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(TestProcessRemoteMessage.class))).thenReturn(Result.success(message));
            var request = PostDspRequest.Builder.newInstance(TestProcessRemoteMessage.class, Object.class, TestError.class)
                    .token("token")
                    .processId("processId")
                    .expectedMessageType("expected-message-type")
                    .message(jsonMessage)
                    .serviceCall(serviceCall)
                    .protocol(protocol)
                    .participantContextProvider(participantContextSupplier)
                    .errorProvider(TestError.Builder::newInstance)
                    .build();

            var result = handler.updateResource(request);

            assertThat(result.getStatus()).isEqualTo(200);
            assertThat(result.getMediaType()).isEqualTo(APPLICATION_JSON_TYPE);
            assertThat(message.getProtocol()).isEqualTo(DATASPACE_PROTOCOL_HTTP);
            assertThat(message.getProcessId()).isEqualTo("processId");
            verify(validatorRegistry).validate("expected-message-type", jsonMessage);
            verify(transformerRegistry).transform(jsonMessage, TestProcessRemoteMessage.class);
        }

        @Test
        void shouldFail_whenTokenIsNotValid() {
            var jsonMessage = Json.createObjectBuilder().build();
            var request = postDspRequestBuilder()
                    .processId("processId")
                    .message(jsonMessage)
                    .serviceCall((ctx, m, t) -> ServiceResult.unauthorized("unauthorized"))
                    .build();
            var message = TestProcessRemoteMessage.Builder.newInstance().providerPid("providerPid").consumerPid("processId").build();
            var jsonError = error("401", "unauthorized", request.getProcessId());
            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), eq(TestProcessRemoteMessage.class))).thenReturn(Result.success(message));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));

            var result = handler.updateResource(request);

            assertThat(result.getStatus()).isEqualTo(401);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("TestError");
                assertThat(error.getString(DSPACE_PROPERTY_CODE_IRI)).isEqualTo("401");
                assertThat(error.getString(DSPACE_PROPERTY_PROCESS_ID)).isEqualTo("processId");
                assertThat(error.get(DSPACE_PROPERTY_REASON_IRI)).isNotNull();
            });
        }

        @Test
        void shouldReturnUnauthorized_whenTokenIsNull() {
            var jsonMessage = Json.createObjectBuilder().build();
            var request = postDspRequestBuilder()
                    .token(null)
                    .processId("processId")
                    .message(jsonMessage)
                    .serviceCall((ctx, m, t) -> ServiceResult.unauthorized("unauthorized"))
                    .build();

            var jsonError = error("401", "unauthorized", request.getProcessId());
            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));

            var result = handler.updateResource(request);

            assertThat(result.getStatus()).isEqualTo(401);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("TestError");
                assertThat(error.getString(DSPACE_PROPERTY_CODE_IRI)).isEqualTo("401");
                assertThat(error.getString(DSPACE_PROPERTY_PROCESS_ID)).isEqualTo("processId");
                assertThat(error.get(DSPACE_PROPERTY_REASON_IRI)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenValidationFails() {
            var request = postDspRequestBuilder().processId("processId").build();
            var jsonError = error("400", "Failure", request.getProcessId());

            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("error", "path")));
            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));
            var result = handler.updateResource(request);

            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("TestError");
                assertThat(error.getString(DSPACE_PROPERTY_CODE_IRI)).isEqualTo("400");
                assertThat(error.getString(DSPACE_PROPERTY_PROCESS_ID)).isEqualTo("processId");
                assertThat(error.get(DSPACE_PROPERTY_REASON_IRI)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenTransformationFails() {
            var request = postDspRequestBuilder().processId("processId").build();
            var jsonError = error("400", "Failure", request.getProcessId());

            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));


            var result = handler.updateResource(request);

            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("TestError");
                assertThat(error.getString(DSPACE_PROPERTY_CODE_IRI)).isEqualTo("400");
                assertThat(error.getString(DSPACE_PROPERTY_PROCESS_ID)).isEqualTo("processId");
                assertThat(error.get(DSPACE_PROPERTY_REASON_IRI)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenProtocolParseFails() {
            var faultyProtocol = "faultyProtocol";
            when(dspTransformerRegistry.forProtocol(faultyProtocol)).thenReturn(Result.failure(faultyProtocol));

            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            var request = postDspRequestBuilder().protocol(faultyProtocol).build();

            assertThatThrownBy(() -> handler.createResource(request)).isInstanceOf(EdcException.class);

        }

        @Test
        void shouldFail_whenIdIsNotValid() {
            var message = TestProcessRemoteMessage.Builder.newInstance()
                    .consumerPid("consumerPid")
                    .providerPid("providerPid")
                    .build();
            var request = postDspRequestBuilder().processId("anotherId").build();
            var jsonError = error("400", "Failure", request.getProcessId());

            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(message));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));

            var result = handler.updateResource(request);

            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("TestError");
                assertThat(error.getString(DSPACE_PROPERTY_CODE_IRI)).isEqualTo("400");
                assertThat(error.getString(DSPACE_PROPERTY_PROCESS_ID)).isEqualTo("anotherId");
                assertThat(error.get(DSPACE_PROPERTY_REASON_IRI)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenServiceCallFails() {
            ServiceCall<TestProcessRemoteMessage, Object> serviceCall = (ctx, m, t) -> ServiceResult.conflict("error");
            var message = TestProcessRemoteMessage.Builder.newInstance().providerPid("providerPid").consumerPid("processId").build();
            var request = postDspRequestBuilder().processId("processId").serviceCall(serviceCall).build();
            var jsonError = error("409", "Failure", request.getProcessId());

            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.success(message));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(jsonError));


            var result = handler.updateResource(request);

            assertThat(result.getStatus()).isEqualTo(409);
            assertThat(result.getEntity()).asInstanceOf(type(JsonObject.class)).satisfies(error -> {
                assertThat(error.getString(TYPE)).isEqualTo("TestError");
                assertThat(error.getString(DSPACE_PROPERTY_CODE_IRI)).isEqualTo("409");
                assertThat(error.getString(DSPACE_PROPERTY_PROCESS_ID)).isEqualTo("processId");
                assertThat(error.get(DSPACE_PROPERTY_REASON_IRI)).isNotNull();
            });
        }

        @Test
        void shouldFail_whenParticipantContextSupplierFails() {
            var resourceJson = Json.createObjectBuilder().build();

            when(dspTransformerRegistry.forProtocol(protocol)).thenReturn(Result.success(transformerRegistry));
            when(transformerRegistry.transform(isA(TestError.class), eq(JsonObject.class))).thenReturn(Result.success(resourceJson));

            var request = postDspRequestBuilder()
                    .participantContextProvider(() -> ServiceResult.notFound("not found"))
                    .build();

            var result = handler.updateResource(request);

            assertThat(result.getStatus()).isEqualTo(401);

        }

        private PostDspRequest.Builder<TestProcessRemoteMessage, Object, TestError> postDspRequestBuilder() {
            return PostDspRequest.Builder
                    .newInstance(TestProcessRemoteMessage.class, Object.class, TestError.class)
                    .token("token")
                    .protocol(protocol)
                    .errorProvider(TestError.Builder::new)
                    .participantContextProvider(participantContextSupplier)
                    .serviceCall((ctx, i, c) -> ServiceResult.success());
        }

    }

}
