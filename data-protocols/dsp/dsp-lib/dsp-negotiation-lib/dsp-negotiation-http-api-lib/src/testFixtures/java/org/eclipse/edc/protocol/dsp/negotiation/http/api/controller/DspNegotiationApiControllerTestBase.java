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

package org.eclipse.edc.protocol.dsp.negotiation.http.api.controller;

import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.http.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.PostDspRequest;
import org.eclipse.edc.spi.result.ServiceResult;
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
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.CONTRACT_OFFERS;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.EVENT;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.INITIAL_CONTRACT_OFFERS;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.TERMINATION;
import static org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths.VERIFICATION;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class DspNegotiationApiControllerTestBase extends RestControllerTestBase {

    protected final ContractNegotiationProtocolService protocolService = mock();
    protected final DspRequestHandler dspRequestHandler = mock();
    private final ParticipantContext participantContext = ParticipantContext.Builder.newInstance()
            .participantContextId("participantContextId")
            .identity("participantId")
            .build();
    protected final SingleParticipantContextSupplier participantContextSupplier = () -> ServiceResult.success(participantContext);

    @Test
    void getNegotiation_shouldGetResource() {
        when(dspRequestHandler.getResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());

        var result = baseRequest()
                .get(basePath() + "negotiationId")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(200);

        assertThat(result).isNotNull();
        var captor = ArgumentCaptor.forClass(GetDspRequest.class);
        verify(dspRequestHandler).getResource(captor.capture());
        var dspMessage = captor.getValue();
        assertThat(dspMessage.getToken()).isEqualTo("auth");
        assertThat(dspMessage.getId()).isEqualTo("negotiationId");
        assertThat(dspMessage.getResultClass()).isEqualTo(ContractNegotiation.class);
    }

    @Test
    void initialContractRequest_shouldCreateResource() {
        var requestBody = createObjectBuilder().add("@type", namespace().toIri(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM)).build();
        when(dspRequestHandler.createResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());

        var result = baseRequest()
                .contentType(APPLICATION_JSON)
                .body(requestBody)
                .post(basePath() + INITIAL_CONTRACT_REQUEST)
                .then()
                .statusCode(200)
                .contentType(APPLICATION_JSON);

        assertThat(result).isNotNull();
        var captor = ArgumentCaptor.forClass(PostDspRequest.class);
        verify(dspRequestHandler).createResource(captor.capture());
        var request = captor.getValue();
        assertThat(request.getToken()).isEqualTo("auth");
        assertThat(request.getProcessId()).isEqualTo(null);
        assertThat(request.getMessage()).isNotNull();
        assertThat(request.getInputClass()).isEqualTo(ContractRequestMessage.class);
        assertThat(request.getResultClass()).isEqualTo(ContractNegotiation.class);
        assertThat(request.getExpectedMessageType()).isEqualTo(namespace().toIri(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM));
    }

    @Test
    void initialContractOffer_shouldCreateResource() {
        var requestBody = createObjectBuilder().add("@type", namespace().toIri(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM)).build();
        when(dspRequestHandler.createResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());

        var result = baseRequest()
                .contentType(APPLICATION_JSON)
                .body(requestBody)
                .post(basePath() + initialOffersPath())
                .then()
                .statusCode(200)
                .contentType(APPLICATION_JSON);

        assertThat(result).isNotNull();
        var captor = ArgumentCaptor.forClass(PostDspRequest.class);
        verify(dspRequestHandler).createResource(captor.capture());
        var request = captor.getValue();
        assertThat(request.getToken()).isEqualTo("auth");
        assertThat(request.getProcessId()).isEqualTo(null);
        assertThat(request.getMessage()).isNotNull();
        assertThat(request.getInputClass()).isEqualTo(ContractOfferMessage.class);
        assertThat(request.getResultClass()).isEqualTo(ContractNegotiation.class);
        assertThat(request.getExpectedMessageType()).isEqualTo(namespace().toIri(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM));
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
                .contentType(APPLICATION_JSON)
                .body(requestBody)
                .post(basePath() + path)
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(200);

        var captor = ArgumentCaptor.forClass(PostDspRequest.class);
        verify(dspRequestHandler).updateResource(captor.capture());
        var request = captor.getValue();
        assertThat(request.getExpectedMessageType()).isEqualTo(namespace().toIri(messageType));
        assertThat(request.getToken()).isEqualTo("auth");
        assertThat(request.getProcessId()).isEqualTo("testId");
        assertThat(request.getMessage()).isNotNull();
        assertThat(request.getInputClass()).isEqualTo(messageClass);
        assertThat(request.getResultClass()).isEqualTo(ContractNegotiation.class);
    }

    @Test
    void callEndpoint_shouldUpdateResource_whenContractOfferMessage() {
        when(dspRequestHandler.updateResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());
        var requestBody = createObjectBuilder().add("http://schema/key", "value").build();

        baseRequest()
                .contentType(APPLICATION_JSON)
                .body(requestBody)
                .post(basePath() + "testId" + offersPath())
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(200);

        var captor = ArgumentCaptor.forClass(PostDspRequest.class);
        verify(dspRequestHandler).updateResource(captor.capture());
        var request = captor.getValue();
        assertThat(request.getExpectedMessageType()).isEqualTo(namespace().toIri(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM));
        assertThat(request.getToken()).isEqualTo("auth");
        assertThat(request.getProcessId()).isEqualTo("testId");
        assertThat(request.getMessage()).isNotNull();
        assertThat(request.getInputClass()).isEqualTo(ContractOfferMessage.class);
        assertThat(request.getResultClass()).isEqualTo(ContractNegotiation.class);
    }

    protected abstract String basePath();

    protected abstract JsonLdNamespace namespace();


    protected String offersPath() {
        return CONTRACT_OFFERS;
    }

    protected String initialOffersPath() {
        return INITIAL_CONTRACT_OFFERS;
    }

    private RequestSpecification baseRequest() {
        var authHeader = "auth";
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .when();
    }

    private static class ControllerMethodArguments implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(
                            "testId" + CONTRACT_REQUEST,
                            ContractRequestMessage.class,
                            DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM),
                    Arguments.of(
                            "testId" + EVENT,
                            ContractNegotiationEventMessage.class,
                            DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM),
                    Arguments.of(
                            "testId" + AGREEMENT + VERIFICATION,
                            ContractAgreementVerificationMessage.class,
                            DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_TERM),
                    Arguments.of(
                            "testId" + TERMINATION,
                            ContractNegotiationTerminationMessage.class,
                            DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM),
                    Arguments.of(
                            "testId" + AGREEMENT,
                            ContractAgreementMessage.class,
                            DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM)
            );
        }
    }
}



