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

package org.eclipse.edc.protocol.dsp.transferprocess.http.api.controller;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferSuspensionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.http.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.PostDspRequest;
import org.eclipse.edc.protocol.dsp.spi.type.DspNamespace;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Nested;
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
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2024_1_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.TRANSFER_COMPLETION;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.TRANSFER_INITIAL_REQUEST;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.TRANSFER_START;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.TRANSFER_SUSPENSION;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.TRANSFER_TERMINATION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
class DspTransferProcessApiControllerTest {

    abstract static class Tests extends RestControllerTestBase {

        private static final String PROCESS_ID = "testId";
        protected final TransferProcessProtocolService protocolService = mock();
        protected final DspRequestHandler dspRequestHandler = mock();

        @Test
        void getTransferProcess_shouldGetResource() {
            var id = "transferProcessId";

            when(dspRequestHandler.getResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());

            baseRequest()
                    .get(basePath() + id)
                    .then()
                    .contentType(MediaType.APPLICATION_JSON)
                    .statusCode(200);

            var captor = ArgumentCaptor.forClass(GetDspRequest.class);
            verify(dspRequestHandler).getResource(captor.capture());
            var dspRequest = captor.getValue();
            assertThat(dspRequest.getId()).isEqualTo("transferProcessId");
            assertThat(dspRequest.getResultClass()).isEqualTo(TransferProcess.class);
            assertThat(dspRequest.getToken()).isEqualTo("auth");
        }

        @Test
        void initiateTransferProcess_shouldCreateResource() {
            when(dspRequestHandler.createResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());

            var result = baseRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Json.createObjectBuilder().add(TYPE, namespace().toIri(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM)).build())
                    .post(basePath() + TRANSFER_INITIAL_REQUEST)
                    .then()
                    .statusCode(200)
                    .contentType(MediaType.APPLICATION_JSON);

            assertThat(result).isNotNull();
            var captor = ArgumentCaptor.forClass(PostDspRequest.class);
            verify(dspRequestHandler).createResource(captor.capture());
            var request = captor.getValue();
            assertThat(request.getToken()).isEqualTo("auth");
            assertThat(request.getProcessId()).isEqualTo(null);
            assertThat(request.getInputClass()).isEqualTo(TransferRequestMessage.class);
            assertThat(request.getResultClass()).isEqualTo(TransferProcess.class);
            assertThat(request.getMessage()).isNotNull();
            assertThat(request.getExpectedMessageType()).isEqualTo(namespace().toIri(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM));
        }

        /**
         * Verifies that an endpoint returns 401 if the identity service cannot verify the identity token.
         *
         * @param path the request path to the endpoint
         */
        @ParameterizedTest
        @ArgumentsSource(ControllerMethodArguments.class)
        void callEndpoint_shouldUpdateResource(String path, Class<?> messageClass, String messageType) {
            when(dspRequestHandler.updateResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());
            var requestBody = createObjectBuilder().add("http://schema/key", "value").build();

            baseRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .post(basePath() + path)
                    .then()
                    .contentType(MediaType.APPLICATION_JSON)
                    .statusCode(200);

            var captor = ArgumentCaptor.forClass(PostDspRequest.class);
            verify(dspRequestHandler).updateResource(captor.capture());
            var request = captor.getValue();
            assertThat(request.getToken()).isEqualTo("auth");
            assertThat(request.getProcessId()).isEqualTo(PROCESS_ID);
            assertThat(request.getMessage()).isNotNull();
            assertThat(request.getInputClass()).isEqualTo(messageClass);
            assertThat(request.getResultClass()).isEqualTo(TransferProcess.class);
            assertThat(request.getExpectedMessageType()).isEqualTo(namespace().toIri(messageType));
        }

        protected abstract String basePath();

        protected abstract DspNamespace namespace();

        private RequestSpecification baseRequest() {
            return given()
                    .baseUri("http://localhost:" + port)
                    .basePath("/")
                    .header(HttpHeaders.AUTHORIZATION, "auth")
                    .when();
        }

        private static class ControllerMethodArguments implements ArgumentsProvider {
            @Override
            public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        Arguments.of(
                                PROCESS_ID + TRANSFER_START,
                                TransferStartMessage.class,
                                DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM),
                        Arguments.of(
                                PROCESS_ID + TRANSFER_COMPLETION,
                                TransferCompletionMessage.class,
                                DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM),
                        Arguments.of(
                                PROCESS_ID + TRANSFER_SUSPENSION,
                                TransferSuspensionMessage.class,
                                DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_TERM),
                        Arguments.of(
                                PROCESS_ID + TRANSFER_TERMINATION,
                                TransferTerminationMessage.class,
                                DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM)
                );
            }
        }

    }

    @ApiTest
    @Nested
    class DspTransferProcessApiControllerV08Test extends Tests {

        @Override
        protected String basePath() {
            return BASE_PATH;
        }

        @Override
        protected DspNamespace namespace() {
            return DSP_NAMESPACE_V_08;
        }

        @Override
        protected Object controller() {
            return new DspTransferProcessApiController(protocolService, dspRequestHandler);
        }
    }

    @ApiTest
    @Nested
    class DspTransferProcessControllerV20241Test extends Tests {

        @Override
        protected String basePath() {
            return V_2024_1_PATH + BASE_PATH;
        }

        @Override
        protected DspNamespace namespace() {
            return DSP_NAMESPACE_V_2024_1;
        }

        @Override
        protected Object controller() {
            return new DspTransferProcessApiController20241(protocolService, dspRequestHandler);
        }
    }
}
