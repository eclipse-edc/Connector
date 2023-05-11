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

package org.eclipse.edc.protocol.dsp.negotiation.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.protocol.dsp.negotiation.transform.ContractNegotiationError;
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
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage.Type.ACCEPTED;
import static org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage.Type.FINALIZED;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.CONTRACT_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.EVENT;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.TERMINATION;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.VERIFICATION;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_CONTRACT_NEGOTIATION_ERROR;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_AGREEMENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_EVENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_REASON;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_TERMINATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
public class DspNegotiationApiControllerTest extends RestControllerTestBase {

    private final ObjectMapper mapper = mock(ObjectMapper.class);
    private final IdentityService identityService = mock(IdentityService.class);
    private final TypeTransformerRegistry registry = mock(TypeTransformerRegistry.class);
    private final ContractNegotiationProtocolService protocolService = mock(ContractNegotiationProtocolService.class);

    private final String callbackAddress = "http://callback";
    private final JsonObject request = Json.createObjectBuilder()
            .add("http://schema/key", "value")
            .build();
    private final JsonLd jsonLdService = new TitaniumJsonLd(mock(Monitor.class));

    private static ClaimToken token() {
        return ClaimToken.Builder.newInstance().build();
    }

    private static ContractAgreementMessage contractAgreementMessage() {
        return ContractAgreementMessage.Builder.newInstance()
                .protocol("protocol")
                .processId("testId")
                .counterPartyAddress("http://connector")
                .contractAgreement(contractAgreement())
                .build();
    }

    private static ContractAgreement contractAgreement() {
        return ContractAgreement.Builder.newInstance()
                .id(randomUUID().toString())
                .providerId("agentId")
                .consumerId("agentId")
                .assetId("assetId")
                .policy(policy()).build();
    }

    private static ContractRequestMessage contractRequestMessage() {
        return ContractRequestMessage.Builder.newInstance()
                .protocol("protocol")
                .processId("testId")
                .counterPartyAddress("http://connector")
                .callbackAddress("http://connector")
                .dataSet("dataSet")
                .contractOffer(contractOffer())
                .build();
    }

    private static ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance()
                .id(String.valueOf(randomUUID()))
                .assetId("assetId")
                .policy(policy()).build();
    }

    private static Policy policy() {
        var action = Action.Builder.newInstance().type("USE").build();
        var permission = Permission.Builder.newInstance().action(action).build();
        var prohibition = Prohibition.Builder.newInstance().action(action).build();
        var duty = Duty.Builder.newInstance().action(action).build();
        return Policy.Builder.newInstance()
                .permission(permission)
                .prohibition(prohibition)
                .duty(duty)
                .build();
    }

    private static ContractAgreementVerificationMessage contractAgreementVerificationMessage() {
        return ContractAgreementVerificationMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://connector")
                .processId("testId")
                .build();
    }

    private static ContractNegotiationEventMessage contractNegotiationEventMessage_accepted() {
        return ContractNegotiationEventMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://connector")
                .processId("testId")
                .type(ACCEPTED)
                .build();
    }

    private static ContractNegotiationEventMessage contractNegotiationEventMessage_finalized() {
        return ContractNegotiationEventMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://connector")
                .processId("testId")
                .type(FINALIZED)
                .build();
    }

    private static ContractNegotiationTerminationMessage contractNegotiationTerminationMessage() {
        return ContractNegotiationTerminationMessage.Builder.newInstance()
                .protocol("protocol")
                .counterPartyAddress("http://connector")
                .processId("testId")
                .rejectionReason("reason")
                .build();
    }

    private static Response contractNegotiationErrorResponseUnsupperted() {
        var builder = Json.createObjectBuilder();

        builder.add(JsonLdKeywords.TYPE, DSPACE_CONTRACT_NEGOTIATION_ERROR);
        builder.add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, "testID");
        builder.add(DSPACE_NEGOTIATION_PROPERTY_CODE, "501");
        builder.add(DSPACE_NEGOTIATION_PROPERTY_REASON, Json.createArrayBuilder().add("reasonTest"));

        return Response.status(501).entity(builder.build()).build();
    }

    private static Response contractNegotiationErrorResponseTransformationFailed() {
        var builder = Json.createObjectBuilder();

        builder.add(JsonLdKeywords.TYPE, DSPACE_CONTRACT_NEGOTIATION_ERROR);
        builder.add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, "testID");
        builder.add(DSPACE_NEGOTIATION_PROPERTY_CODE, "500");
        builder.add(DSPACE_NEGOTIATION_PROPERTY_REASON, Json.createArrayBuilder().add("reasonTest"));

        return Response.status(500).entity(builder.build()).build();
    }

    private static Response contractNegotiationErrorResponseNotAuthorized() {
        var builder = Json.createObjectBuilder();

        builder.add(JsonLdKeywords.TYPE, DSPACE_CONTRACT_NEGOTIATION_ERROR);
        builder.add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, "testID");
        builder.add(DSPACE_NEGOTIATION_PROPERTY_CODE, "401");
        builder.add(DSPACE_NEGOTIATION_PROPERTY_REASON, Json.createArrayBuilder().add("reasonTest"));

        return Response.status(401).entity(builder.build()).build();
    }

    private static Response contractNegotiationErrorResponseBadRequest() {
        var builder = Json.createObjectBuilder();

        builder.add(JsonLdKeywords.TYPE, DSPACE_CONTRACT_NEGOTIATION_ERROR);
        builder.add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, "testID");
        builder.add(DSPACE_NEGOTIATION_PROPERTY_CODE, "400");
        builder.add(DSPACE_NEGOTIATION_PROPERTY_REASON, Json.createArrayBuilder().add("reasonTest"));

        return Response.status(400).entity(builder.build()).build();
    }

    private static Response contractNegotiationErrorResponseConflict() {
        var builder = Json.createObjectBuilder();

        builder.add(JsonLdKeywords.TYPE, DSPACE_CONTRACT_NEGOTIATION_ERROR);
        builder.add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, "testID");
        builder.add(DSPACE_NEGOTIATION_PROPERTY_CODE, "409");
        builder.add(DSPACE_NEGOTIATION_PROPERTY_REASON, Json.createArrayBuilder().add("reasonTest"));

        return Response.status(409).entity(builder.build()).build();
    }

    @Test
    void getNegotiation_shouldReturnNotImplemented_whenOperationNotSupported() {
        var token = token();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress))).thenReturn(Result.success(token));
        when(registry.transform(any(ContractNegotiationError.class), eq(Response.class))).thenReturn(Result.success(contractNegotiationErrorResponseUnsupperted()));

        //operation not yet supported
        var result = baseRequest()
                .get(BASE_PATH + "testId")
                .then()
                .statusCode(501)
                .extract().as(Map.class);

        assertThat(result.get(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_CONTRACT_NEGOTIATION_ERROR);
        assertThat(result.get(DSPACE_SCHEMA + "code")).isEqualTo("501");
        assertThat(result.get(DSPACE_SCHEMA + "reason")).isNotNull();
    }

    @Test
    void initiateNegotiation_shouldReturnTransferProcess_whenValidRequest() {
        var token = token();
        var message = contractRequestMessage();
        var process = contractNegotiation();
        var json = Json.createObjectBuilder().build();
        var map = Map.of("key", "value");
        var request = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE).build();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress))).thenReturn(Result.success(token));
        when(registry.transform(any(JsonObject.class), eq(ContractRequestMessage.class))).thenReturn(Result.success(message));
        when(protocolService.notifyRequested(message, token)).thenReturn(ServiceResult.success(process));
        when(registry.transform(any(ContractNegotiation.class), eq(JsonObject.class))).thenReturn(Result.success(json));
        when(mapper.convertValue(any(JsonObject.class), eq(Map.class))).thenReturn(map);

        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(BASE_PATH + INITIAL_CONTRACT_REQUEST)
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().as(Map.class);

        assertThat(result).isEqualTo(map);
        verify(protocolService, times(1)).notifyRequested(message, token);
    }

    @Test
    void initiateNegotiation_shouldReturnInternalServerError_whenContractNegotiationTransformationFails() {
        var token = token();
        var message = contractRequestMessage();
        var process = contractNegotiation();
        var request = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE).build();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress))).thenReturn(Result.success(token));
        when(registry.transform(any(JsonObject.class), eq(ContractRequestMessage.class))).thenReturn(Result.success(message));
        when(protocolService.notifyRequested(message, token)).thenReturn(ServiceResult.success(process));
        when(registry.transform(any(ContractNegotiation.class), eq(JsonObject.class))).thenReturn(Result.failure("error"));
        when(registry.transform(any(ContractNegotiationError.class), eq(Response.class))).thenReturn(Result.success(contractNegotiationErrorResponseTransformationFailed()));

        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(BASE_PATH + INITIAL_CONTRACT_REQUEST)
                .then()
                .statusCode(500)
                .extract().as(Map.class);

        assertThat(result.get(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_CONTRACT_NEGOTIATION_ERROR);
        assertThat(result.get(DSPACE_SCHEMA + "code")).isEqualTo("500");
        assertThat(result.get(DSPACE_SCHEMA + "reason")).isNotNull();
    }

    @Test
    void initiateNegotiation_shouldReturnInternalServerError_whenConvertingResultFails() {
        var token = token();
        var message = contractRequestMessage();
        var process = contractNegotiation();
        var json = Json.createObjectBuilder().build();
        var request = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE).build();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress))).thenReturn(Result.success(token));
        when(registry.transform(any(JsonObject.class), eq(ContractRequestMessage.class))).thenReturn(Result.success(message));
        when(protocolService.notifyRequested(message, token)).thenReturn(ServiceResult.success(process));
        when(registry.transform(any(ContractNegotiation.class), eq(JsonObject.class))).thenReturn(Result.success(json));
        when(mapper.convertValue(any(JsonObject.class), eq(Map.class))).thenThrow(IllegalArgumentException.class);
        when(registry.transform(any(ContractNegotiationError.class), eq(Response.class))).thenReturn(Result.success(contractNegotiationErrorResponseTransformationFailed()));

        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(BASE_PATH + INITIAL_CONTRACT_REQUEST)
                .then()
                .contentType(MediaType.APPLICATION_JSON)
                .statusCode(500)
                .extract().as(Map.class);

        assertThat(result.get(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_CONTRACT_NEGOTIATION_ERROR);
        assertThat(result.get(DSPACE_SCHEMA + "code")).isEqualTo("500");
    }

    @Test
    void providerOffer_shouldReturnNotImplemented_whenOperationNotSupported() {
        var token = token();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress))).thenReturn(Result.success(token));
        when(registry.transform(any(ContractNegotiationError.class), eq(Response.class))).thenReturn(Result.success(contractNegotiationErrorResponseUnsupperted()));

        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(BASE_PATH + "testId" + CONTRACT_OFFER)
                .then()
                .statusCode(501)
                .extract().as(Map.class);

        assertThat(result.get(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_CONTRACT_NEGOTIATION_ERROR);
        assertThat(result.get(DSPACE_SCHEMA + "code")).isEqualTo("501");
        assertThat(result.get(DSPACE_SCHEMA + "reason")).isNotNull();
    }

    /**
     * Verifies that an endpoint returns 401 if the identity service cannot verify the identity token.
     *
     * @param path the request path to the endpoint
     */
    @ParameterizedTest
    @ArgumentsSource(ControllerMethodArguments.class)
    void callEndpoint_shouldReturnUnauthorized_whenNotAuthorized(String path) {
        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress))).thenReturn(Result.failure("error"));
        when(registry.transform(any(ContractNegotiationError.class), eq(Response.class))).thenReturn(Result.success(contractNegotiationErrorResponseNotAuthorized()));

        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(path)
                .then()
                .statusCode(401)
                .extract().as(Map.class);

        assertThat(result.get(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_CONTRACT_NEGOTIATION_ERROR);
        assertThat(result.get(DSPACE_SCHEMA + "code")).isEqualTo("401");
        assertThat(result.get(DSPACE_SCHEMA + "reason")).isNotNull();
    }

    /**
     * Verifies that an endpoint returns 400 if the incoming message cannot be transformed.
     *
     * @param path the request path to the endpoint
     */
    @ParameterizedTest
    @ArgumentsSource(ControllerMethodArguments.class)
    void callEndpoint_shouldReturnBadRequest_whenRequestTransformationFails(String path) {
        var token = token();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress))).thenReturn(Result.success(token));
        when(registry.transform(any(JsonObject.class), argThat(ContractRemoteMessage.class::isAssignableFrom))).thenReturn(Result.failure("error"));
        when(registry.transform(any(ContractNegotiationError.class), eq(Response.class))).thenReturn(Result.success(contractNegotiationErrorResponseBadRequest()));


        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(path)
                .then()
                .statusCode(400)
                .extract().as(Map.class);

        assertThat(result.get(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_CONTRACT_NEGOTIATION_ERROR);
        assertThat(result.get(DSPACE_SCHEMA + "code")).isEqualTo("400");
        assertThat(result.get(DSPACE_SCHEMA + "reason")).isNotNull();
    }

    /**
     * Verifies that an endpoint returns 200 if the call to the service was successful. Also verifies
     * that the correct service method was called. This is only applicable for endpoints that do not
     * return a response body.
     *
     * @param path          the request path to the endpoint
     * @param message       the request message to be returned by the transformer registry
     * @param request       the request body for a defined endpoint
     * @param serviceMethod reference to the service method that should be called, required to verify that it was called
     */
    @ParameterizedTest
    @ArgumentsSource(ControllerMethodArgumentsForServiceCall.class)
    void callEndpoint_shouldCallService_whenValidRequest(String path, ContractRemoteMessage message, JsonObject request, Method serviceMethod) throws Exception {
        var token = token();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress))).thenReturn(Result.success(token));
        when(registry.transform(any(JsonObject.class), argThat(ContractRemoteMessage.class::isAssignableFrom))).thenReturn(Result.success(message));

        when(protocolService.notifyRequested(any(ContractRequestMessage.class), eq(token))).thenReturn(ServiceResult.success(contractNegotiation()));
        when(protocolService.notifyFinalized(any(ContractNegotiationEventMessage.class), eq(token))).thenReturn(ServiceResult.success(contractNegotiation()));
        when(protocolService.notifyAccepted(any(ContractNegotiationEventMessage.class), eq(token))).thenReturn(ServiceResult.success(contractNegotiation()));
        when(protocolService.notifyVerified(any(ContractAgreementVerificationMessage.class), eq(token))).thenReturn(ServiceResult.success(contractNegotiation()));
        when(protocolService.notifyTerminated(any(ContractNegotiationTerminationMessage.class), eq(token))).thenReturn(ServiceResult.success(contractNegotiation()));
        when(protocolService.notifyAgreed(any(ContractAgreementMessage.class), eq(token))).thenReturn(ServiceResult.success(contractNegotiation()));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(path)
                .then()
                .statusCode(200);

        var verify = verify(protocolService, times(1));
        serviceMethod.invoke(verify, message, token);

        // verify that the message protocol was set to the DSP protocol by the controller
        assertThat(message.getProtocol()).isEqualTo(DATASPACE_PROTOCOL_HTTP);

    }

    /**
     * Verifies that an endpoint returns 500 if there is an error in the service. Also verifies
     * that the correct service method was called.
     *
     * @param path          the request path to the endpoint
     * @param message       the request message to be returned by the transformer registry
     * @param request       the request body for a defined endpoint
     * @param serviceMethod reference to the service method that should be called, required to verify that it was called
     */
    @ParameterizedTest
    @ArgumentsSource(ControllerMethodArgumentsForServiceError.class)
    void callEndpoint_shouldReturnConflict_whenServiceResultConflict(String path, ContractRemoteMessage message, JsonObject request, Method serviceMethod) throws Exception {
        var token = token();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress))).thenReturn(Result.success(token));
        when(registry.transform(any(JsonObject.class), argThat(ContractRemoteMessage.class::isAssignableFrom))).thenReturn(Result.success(message));

        when(protocolService.notifyRequested(any(ContractRequestMessage.class), eq(token))).thenReturn(ServiceResult.conflict("error"));
        when(protocolService.notifyFinalized(any(ContractNegotiationEventMessage.class), eq(token))).thenReturn(ServiceResult.conflict("error"));
        when(protocolService.notifyAccepted(any(ContractNegotiationEventMessage.class), eq(token))).thenReturn(ServiceResult.conflict("error"));
        when(protocolService.notifyVerified(any(ContractAgreementVerificationMessage.class), eq(token))).thenReturn(ServiceResult.conflict("error"));
        when(protocolService.notifyTerminated(any(ContractNegotiationTerminationMessage.class), eq(token))).thenReturn(ServiceResult.conflict("error"));
        when(protocolService.notifyAgreed(any(ContractAgreementMessage.class), eq(token))).thenReturn(ServiceResult.conflict("error"));
        when(registry.transform(any(ContractNegotiationError.class), eq(Response.class))).thenReturn(Result.success(contractNegotiationErrorResponseConflict()));

        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(path)
                .then()
                .statusCode(409)
                .extract().as(Map.class);

        var verify = verify(protocolService, times(1));
        serviceMethod.invoke(verify, message, token);

        assertThat(result.get(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_CONTRACT_NEGOTIATION_ERROR);
        assertThat(result.get(DSPACE_SCHEMA + "code")).isEqualTo("409");
        assertThat(result.get(DSPACE_SCHEMA + "reason")).isNotNull();

        // verify that the message protocol was set to the DSP protocol by the controller
        assertThat(message.getProtocol()).isEqualTo(DATASPACE_PROTOCOL_HTTP);

    }

    /**
     * Verifies that an endpoint returns 500 if there is an error in the service. Also verifies
     * that the correct service method was called.
     *
     * @param path    the request path to the endpoint
     * @param message the request message to be returned by the transformer registry
     * @param request the request body for a defined endpoint
     */
    @ParameterizedTest
    @ArgumentsSource(ControllerMethodArgumentsForIdValidationFails.class)
    void callEndpoint_shouldReturnBadRequest_whenValidationFails(String path, ContractRemoteMessage message, JsonObject request) throws Exception {
        var token = token();

        when(identityService.verifyJwtToken(any(TokenRepresentation.class), eq(callbackAddress))).thenReturn(Result.success(token));
        when(registry.transform(any(JsonObject.class), argThat(ContractRemoteMessage.class::isAssignableFrom))).thenReturn(Result.success(message));
        when(registry.transform(any(ContractNegotiationError.class), eq(Response.class))).thenReturn(Result.success(contractNegotiationErrorResponseBadRequest()));

        var result = baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(path)
                .then()
                .statusCode(400)
                .extract().as(Map.class);

        assertThat(result.get(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_CONTRACT_NEGOTIATION_ERROR);
        assertThat(result.get(DSPACE_SCHEMA + "code")).isEqualTo("400");
        assertThat(result.get(DSPACE_SCHEMA + "reason")).isNotNull();
                .statusCode(400);

        // verify that the message protocol was set to the DSP protocol by the controller
        assertThat(message.getProtocol()).isEqualTo(DATASPACE_PROTOCOL_HTTP);
    }

    @Override
    protected Object controller() {
        return new DspNegotiationApiController(callbackAddress, identityService, registry, protocolService, jsonLdService, mapper, mock(Monitor.class));
    }

    private RequestSpecification baseRequest() {
        String authHeader = "auth";
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .when();
    }

    private ContractNegotiation contractNegotiation() {
        return ContractNegotiation.Builder.newInstance()
                .id(randomUUID().toString())
                .type(ContractNegotiation.Type.PROVIDER)
                .correlationId("testId")
                .counterPartyId("connectorId")
                .counterPartyAddress("callbackAddress")
                .protocol("protocol")
                .state(400)
                .stateTimestamp(Instant.now().toEpochMilli())
                .build();
    }

    private static class ControllerMethodArguments implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(BASE_PATH + INITIAL_CONTRACT_REQUEST),
                    Arguments.of(BASE_PATH + "testId" + CONTRACT_REQUEST),
                    Arguments.of(BASE_PATH + "testId" + EVENT),
                    Arguments.of(BASE_PATH + "testId" + AGREEMENT + VERIFICATION),
                    Arguments.of(BASE_PATH + "testId" + TERMINATION),
                    Arguments.of(BASE_PATH + "testId" + AGREEMENT)
            );
        }
    }

    private static class ControllerMethodArgumentsForServiceCall implements ArgumentsProvider {
        JsonObject contractRequest = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE).build();
        JsonObject negotiationEvent = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_EVENT_MESSAGE).build();
        JsonObject agreementVerification = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE).build();
        JsonObject negotiationTermination = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_TERMINATION_MESSAGE).build();
        JsonObject contractAgreement = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_AGREEMENT_MESSAGE).build();

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(BASE_PATH + "testId" + CONTRACT_REQUEST, contractRequestMessage(), contractRequest,
                            ContractNegotiationProtocolService.class.getDeclaredMethod("notifyRequested", ContractRequestMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + "testId" + EVENT, contractNegotiationEventMessage_accepted(), negotiationEvent,
                            ContractNegotiationProtocolService.class.getDeclaredMethod("notifyAccepted", ContractNegotiationEventMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + "testId" + EVENT, contractNegotiationEventMessage_finalized(), negotiationEvent,
                            ContractNegotiationProtocolService.class.getDeclaredMethod("notifyFinalized", ContractNegotiationEventMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + "testId" + AGREEMENT + VERIFICATION, contractAgreementVerificationMessage(), agreementVerification,
                            ContractNegotiationProtocolService.class.getDeclaredMethod("notifyVerified", ContractAgreementVerificationMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + "testId" + TERMINATION, contractNegotiationTerminationMessage(), negotiationTermination,
                            ContractNegotiationProtocolService.class.getDeclaredMethod("notifyTerminated", ContractNegotiationTerminationMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + "testId" + AGREEMENT, contractAgreementMessage(), contractAgreement,
                            ContractNegotiationProtocolService.class.getDeclaredMethod("notifyAgreed", ContractAgreementMessage.class, ClaimToken.class))
            );
        }
    }

    private static class ControllerMethodArgumentsForServiceError implements ArgumentsProvider {
        JsonObject contractRequest = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE).build();
        JsonObject negotiationEvent = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_EVENT_MESSAGE).build();
        JsonObject agreementVerification = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE).build();
        JsonObject negotiationTermination = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_TERMINATION_MESSAGE).build();
        JsonObject contractAgreement = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_AGREEMENT_MESSAGE).build();

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(BASE_PATH + INITIAL_CONTRACT_REQUEST, contractRequestMessage(), contractRequest,
                            ContractNegotiationProtocolService.class.getDeclaredMethod("notifyRequested", ContractRequestMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + "testId" + CONTRACT_REQUEST, contractRequestMessage(), contractRequest,
                            ContractNegotiationProtocolService.class.getDeclaredMethod("notifyRequested", ContractRequestMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + "testId" + EVENT, contractNegotiationEventMessage_accepted(), negotiationEvent,
                            ContractNegotiationProtocolService.class.getDeclaredMethod("notifyAccepted", ContractNegotiationEventMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + "testId" + EVENT, contractNegotiationEventMessage_finalized(), negotiationEvent,
                            ContractNegotiationProtocolService.class.getDeclaredMethod("notifyFinalized", ContractNegotiationEventMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + "testId" + AGREEMENT + VERIFICATION, contractAgreementVerificationMessage(), agreementVerification,
                            ContractNegotiationProtocolService.class.getDeclaredMethod("notifyVerified", ContractAgreementVerificationMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + "testId" + TERMINATION, contractNegotiationTerminationMessage(), negotiationTermination,
                            ContractNegotiationProtocolService.class.getDeclaredMethod("notifyTerminated", ContractNegotiationTerminationMessage.class, ClaimToken.class)),
                    Arguments.of(BASE_PATH + "testId" + AGREEMENT, contractAgreementMessage(), contractAgreement,
                            ContractNegotiationProtocolService.class.getDeclaredMethod("notifyAgreed", ContractAgreementMessage.class, ClaimToken.class))
            );
        }
    }

    private static class ControllerMethodArgumentsForIdValidationFails implements ArgumentsProvider {
        JsonObject contractRequest = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE).build();
        JsonObject negotiationEvent = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_EVENT_MESSAGE).build();
        JsonObject agreementVerification = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE).build();
        JsonObject negotiationTermination = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_TERMINATION_MESSAGE).build();
        JsonObject contractAgreement = Json.createObjectBuilder().add("@type", DSPACE_NEGOTIATION_AGREEMENT_MESSAGE).build();

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(BASE_PATH + "invalidId" + CONTRACT_REQUEST, contractRequestMessage(), contractRequest),
                    Arguments.of(BASE_PATH + "invalidId" + EVENT, contractNegotiationEventMessage_accepted(), negotiationEvent),
                    Arguments.of(BASE_PATH + "invalidId" + EVENT, contractNegotiationEventMessage_finalized(), negotiationEvent),
                    Arguments.of(BASE_PATH + "invalidId" + AGREEMENT + VERIFICATION, contractAgreementVerificationMessage(), agreementVerification),
                    Arguments.of(BASE_PATH + "invalidId" + TERMINATION, contractNegotiationTerminationMessage(), negotiationTermination),
                    Arguments.of(BASE_PATH + "invalidId" + AGREEMENT, contractAgreementMessage(), contractAgreement)
            );
        }
    }
}
