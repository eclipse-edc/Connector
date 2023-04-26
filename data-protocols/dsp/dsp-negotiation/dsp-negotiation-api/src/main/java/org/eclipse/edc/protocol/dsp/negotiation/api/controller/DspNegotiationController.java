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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.types.protocol.ContractRemoteMessage;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_SCHEMA;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.compact;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_AGREEMENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_CONTRACT_OFFER_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_EVENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_TERMINATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.CONTRACT_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.EVENT;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.TERMINATION;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.VERIFICATION;
import static org.eclipse.edc.protocol.dsp.transform.util.TypeUtil.isOfExpectedType;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

/**
 * Provides consumer and provider endpoints for the contract negotiation according to the http binding
 * of the dataspace protocol.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path(BASE_PATH)
public class DspNegotiationController {

    private final IdentityService identityService;
    private final JsonLdTransformerRegistry transformerRegistry;
    private final String callbackAddress;
    private final Monitor monitor;
    private final ContractNegotiationProtocolService protocolService;
    private final ObjectMapper mapper;

    public DspNegotiationController(Monitor monitor, ObjectMapper mapper, String callbackAddress,
                                    IdentityService identityService, JsonLdTransformerRegistry transformerRegistry,
                                    ContractNegotiationProtocolService protocolService) {
        this.callbackAddress = callbackAddress;
        this.identityService = identityService;
        this.monitor = monitor;
        this.protocolService = protocolService;
        this.transformerRegistry = transformerRegistry;
        this.mapper = mapper;
    }

    /**
     * Provider-specific endpoint.
     *
     * @param id of contract negotiation.
     * @param token identity token.
     */
    @GET
    @Path("{id}")
    public Map<String, Object> getNegotiation(@PathParam("id") String id, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming request for contract negotiation with id %s", id));

        checkAndReturnAuthToken(token);

        throw new UnsupportedOperationException("Currently not supported.");
    }

    /**
     * Provider-specific endpoint.
     *
     * @param body dspace:ContractRequestMessage sent by a consumer.
     * @param token identity token.
     */
    @POST
    @Path(INITIAL_CONTRACT_REQUEST)
    public Map<String, Object> initiateNegotiation(@RequestBody(description = DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE, required = true) JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug("DSP: Incoming ContractRequestMessage for initiating a contract negotiation.");

        var negotiation = processMessage(token, Optional.empty(), body, DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE, ContractRequestMessage.class, protocolService::notifyRequested);

        var result = transformerRegistry.transform(negotiation, JsonObject.class).orElseThrow(failure ->
                new EdcException(format("Failed to build response: %s", failure.getFailureDetail())));
        return mapper.convertValue(compact(result, jsonLdContext()), Map.class);
    }

    /**
     * Provider-specific endpoint.
     *
     * @param id of contract negotiation.
     * @param body dspace:ContractRequestMessage sent by a consumer.
     * @param token identity token.
     */
    @POST
    @Path("{id}" + CONTRACT_REQUEST)
    public void consumerOffer(@PathParam("id") String id, @RequestBody(description = DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE, required = true) JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractRequestMessage for process %s", id));

        processMessage(token, Optional.of(id), body, DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE, ContractRequestMessage.class, protocolService::notifyRequested);
    }

    /**
     * Endpoint on provider and consumer side.
     *
     * @param id of contract negotiation.
     * @param body dspace:ContractNegotiationEventMessage sent by consumer or provider.
     * @param token identity token.
     */
    @POST
    @Path("{id}" + EVENT)
    public void createEvent(@PathParam("id") String id, @RequestBody(description = DSPACE_NEGOTIATION_EVENT_MESSAGE, required = true) JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractNegotiationEventMessage for process %s", id));

        var claimToken = checkAndReturnAuthToken(token);

        var expanded = expand(body).getJsonObject(0);
        var message = transformerRegistry.transform(hasValidType(expanded, DSPACE_NEGOTIATION_EVENT_MESSAGE), ContractNegotiationEventMessage.class)
                .orElseThrow(failure -> new InvalidRequestException(format("Request body was malformed: %s", failure.getFailureDetail())));

        validateProcessId(message.getProcessId(), id);

        switch (message.getType()) {
            case FINALIZED:
                protocolService.notifyFinalized(message, claimToken).orElseThrow(exceptionMapper(ContractNegotiation.class));
                break;
            case ACCEPTED:
                protocolService.notifyAccepted(message, claimToken).orElseThrow(exceptionMapper(ContractNegotiation.class));
                break;
            default:
                throw new InvalidRequestException(String.format("Cannot process dspace:ContractNegotiationEventMessage with unexpected type %s.", message.getType()));
        }
    }

    /**
     * Provider-specific endpoint.
     *
     * @param id of contract negotiation.
     * @param body dspace:ContractAgreementVerificationMessage sent by a consumer.
     * @param token identity token.
     */
    @POST
    @Path("{id}" + AGREEMENT + VERIFICATION)
    public void verifyAgreement(@PathParam("id") String id, @RequestBody(description = DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE, required = true) JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractAgreementVerificationMessage for process %s", id));

        processMessage(token, Optional.of(id), body, DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE, ContractAgreementVerificationMessage.class, protocolService::notifyVerified);
    }

    /**
     * Endpoint on provider and consumer side.
     *
     * @param id of contract negotiation.
     * @param body dspace:ContractNegotiationTerminationMessage sent by consumer or provider.
     * @param token identity token.
     */
    @POST
    @Path("{id}" + TERMINATION)
    public void terminateNegotiation(@PathParam("id") String id, @RequestBody(description = DSPACE_NEGOTIATION_TERMINATION_MESSAGE, required = true) JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractNegotiationTerminationMessage for process %s", id));

        processMessage(token, Optional.of(id), body, DSPACE_NEGOTIATION_TERMINATION_MESSAGE, ContractNegotiationTerminationMessage.class, protocolService::notifyTerminated);
    }

    /**
     * Consumer-specific endpoint.
     *
     * @param id of contract negotiation.
     * @param body dspace:ContractOfferMessage sent by a provider.
     * @param token identity token.
     */
    @POST
    @Path("{id}" + CONTRACT_OFFER)

    public void providerOffer(@PathParam("id") String id, @RequestBody(description = DSPACE_NEGOTIATION_CONTRACT_OFFER_MESSAGE, required = true) JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractOfferMessage for process %s", id));

        checkAndReturnAuthToken(token);

        throw new UnsupportedOperationException("Currently not supported.");
    }

    /**
     * Consumer-specific endpoint.
     *
     * @param id of contract negotiation.
     * @param body dspace:ContractAgreementMessage sent by a provider.
     * @param token identity token.
     */
    @POST
    @Path("{id}" + AGREEMENT)
    public void createAgreement(@PathParam("id") String id, @RequestBody(description = DSPACE_NEGOTIATION_AGREEMENT_MESSAGE, required = true) JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractAgreementMessage for process %s", id));

        processMessage(token, Optional.of(id), body, DSPACE_NEGOTIATION_AGREEMENT_MESSAGE, ContractAgreementMessage.class, protocolService::notifyAgreed);
    }

    private <M extends ContractRemoteMessage> ContractNegotiation processMessage(String token, Optional<String> processId, JsonObject body, String expectedType, Class<M> messageClass,
                                                                                 BiFunction<M, ClaimToken, ServiceResult<ContractNegotiation>> serviceCall) {
        var claimToken = checkAndReturnAuthToken(token);
        var expanded = expand(body).getJsonObject(0);
        var message = transformerRegistry.transform(hasValidType(expanded, expectedType), messageClass)
                .orElseThrow(failure -> new InvalidRequestException(format("Request body was malformed: %s", failure.getFailureDetail())));

        processId.ifPresent(id -> validateProcessId(message.getProcessId(), id));

        // invokes negotiation protocol service method
        return serviceCall.apply(message, claimToken).orElseThrow(exceptionMapper(ContractNegotiation.class));
    }

    private ClaimToken checkAndReturnAuthToken(String token) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(token).build();
        return identityService.verifyJwtToken(tokenRepresentation, callbackAddress).orElseThrow(failure ->
                new AuthenticationFailedException(format("Authentication failed: %s", failure.getFailureDetail())));
    }

    private void validateProcessId(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new InvalidRequestException(format("Invalid process ID. Expected: %s, actual: %s", expected, actual));
        }
    }

    private JsonObject hasValidType(JsonObject object, String expected) {
        if (!isOfExpectedType(object, expected)) {
            throw new InvalidRequestException(format("Request body was not of expected type: %s", expected));
        }

        return object;
    }

    // TODO refactor according to https://github.com/eclipse-edc/Connector/issues/2763
    private JsonObject jsonLdContext() {
        return Json.createObjectBuilder()
                .add(ODRL_PREFIX, ODRL_SCHEMA)
                .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                .build();
    }
}
