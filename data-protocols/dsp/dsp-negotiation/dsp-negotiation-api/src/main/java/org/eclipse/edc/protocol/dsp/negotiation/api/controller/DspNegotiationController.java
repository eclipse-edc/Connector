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
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.Map;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static java.lang.String.format;
import static org.eclipse.edc.jsonld.JsonLdExtension.TYPE_MANAGER_CONTEXT_JSON_LD;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.compact;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_AGREEMENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_CONTRACT_OFFER_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_EVENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_TERMINATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.CONTRACT_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.EVENT;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.TERMINATION;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.VERIFICATION;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.ODRL_PREFIX;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.ODRL_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.util.TypeUtil.isOfExpectedType;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

/**
 * Provides consumer and provider endpoints according to http binding of the dataspace protocol.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path(BASE_PATH)
public class DspNegotiationController implements DspNegotiationApi {

    private final IdentityService identityService;
    private final JsonLdTransformerRegistry transformerRegistry;
    private final String callbackAddress;
    private final Monitor monitor;
    private final ContractNegotiationService service;
    private final ContractNegotiationProtocolService protocolService;
    private final ObjectMapper mapper;

    public DspNegotiationController(Monitor monitor, TypeManager typeManager, String callbackAddress,
                                    IdentityService identityService, JsonLdTransformerRegistry transformerRegistry,
                                    ContractNegotiationService service, ContractNegotiationProtocolService protocolService) {
        this.callbackAddress = callbackAddress;
        this.identityService = identityService;
        this.monitor = monitor;
        this.protocolService = protocolService;
        this.service = service;
        this.transformerRegistry = transformerRegistry;

        this.mapper = typeManager.getMapper(TYPE_MANAGER_CONTEXT_JSON_LD);
    }

    @GET
    @Path("{id}")
    @Override
    public Map<String, Object> getNegotiation(@PathParam("id") String id, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming request for contract negotiation with id %s", id));

        checkAndReturnAuthToken(token);

        var negotiation = service.findbyId(id);
        if (negotiation == null) {
            throw new ObjectNotFoundException(ContractNegotiation.class, id);
        }

        var result = transformerRegistry.transform(negotiation, JsonObject.class).orElseThrow((failure ->
                new EdcException(format("Failed to build response: %s", failure.getFailureDetail()))));

        return mapper.convertValue(compact(result, jsonLdContext()), Map.class);
    }

    @POST
    @Path(INITIAL_CONTRACT_REQUEST)
    @Override
    public Map<String, Object> initiateNegotiation(@RequestBody(description = DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE, required = true) JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug("DSP: Incoming ContractRequestMessage for initiating a contract negotiation.");

        var claimToken = checkAndReturnAuthToken(token);

        var expanded = expand(body).getJsonObject(0);
        var message = transformerRegistry.transform(hasValidType(expanded, DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE), ContractRequestMessage.class)
                .orElseThrow(failure -> new InvalidRequestException(format("Request body was malformed: %s", failure.getFailureDetail())));

        var negotiation = protocolService.notifyRequested(message, claimToken).orElseThrow(exceptionMapper(ContractNegotiation.class));

        var result = transformerRegistry.transform(negotiation, JsonObject.class).orElseThrow(failure ->
                new EdcException(format("Failed to build response: %s", failure.getFailureDetail())));
        return mapper.convertValue(compact(result, jsonLdContext()), Map.class);
    }

    @POST
    @Path("{id}" + CONTRACT_REQUEST)
    @Override
    public void consumerOffer(@PathParam("id") String id, @RequestBody(description = DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE, required = true) JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractRequestMessage for process %s", id));

        var claimToken = checkAndReturnAuthToken(token);

        var expanded = expand(body).getJsonObject(0);
        var message = transformerRegistry.transform(hasValidType(expanded, DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE), ContractRequestMessage.class)
                .orElseThrow(failure -> new InvalidRequestException(format("Request body was malformed: %s", failure.getFailureDetail())));

        validateId(message.getProcessId(), id);

        protocolService.notifyRequested(message, claimToken).orElseThrow(exceptionMapper(ContractNegotiation.class));
    }

    @POST
    @Path("{id}" + EVENT)
    @Override
    public void createEvent(@PathParam("id") String id, @RequestBody(description = DSPACE_NEGOTIATION_EVENT_MESSAGE, required = true) JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractNegotiationEventMessage for process %s", id));

        var claimToken = checkAndReturnAuthToken(token);

        var expanded = expand(body).getJsonObject(0);
        var message = transformerRegistry.transform(hasValidType(expanded, DSPACE_NEGOTIATION_EVENT_MESSAGE), ContractNegotiationEventMessage.class)
                .orElseThrow(failure -> new InvalidRequestException(format("Request body was malformed: %s", failure.getFailureDetail())));

        validateId(message.getProcessId(), id);

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

    @POST
    @Path("{id}" + AGREEMENT + VERIFICATION)
    @Override
    public void verifyAgreement(@PathParam("id") String id, @RequestBody(description = DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE, required = true) JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractAgreementVerificationMessage for process %s", id));

        var claimToken = checkAndReturnAuthToken(token);

        var expanded = expand(body).getJsonObject(0);
        var message = transformerRegistry.transform(hasValidType(expanded, DSPACE_NEGOTIATION_AGREEMENT_VERIFICATION_MESSAGE), ContractAgreementVerificationMessage.class)
                .orElseThrow(failure -> new InvalidRequestException(format("Request body was malformed: %s", failure.getFailureDetail())));

        validateId(message.getProcessId(), id);

        protocolService.notifyVerified(message, claimToken).orElseThrow(exceptionMapper(ContractNegotiation.class));
    }

    @POST
    @Path("{id}" + TERMINATION)
    @Override
    public void terminateNegotiation(@PathParam("id") String id, @RequestBody(description = DSPACE_NEGOTIATION_TERMINATION_MESSAGE, required = true) JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractNegotiationTerminationMessage for process %s", id));

        var claimToken = checkAndReturnAuthToken(token);

        var expanded = expand(body).getJsonObject(0);
        var message = transformerRegistry.transform(hasValidType(expanded, DSPACE_NEGOTIATION_TERMINATION_MESSAGE), ContractNegotiationTerminationMessage.class)
                .orElseThrow(failure -> new InvalidRequestException(format("Request body was malformed: %s", failure.getFailureDetail())));

        validateId(message.getProcessId(), id);

        protocolService.notifyTerminated(message, claimToken).orElseThrow(exceptionMapper(ContractNegotiation.class));
    }

    @POST
    @Path("{id}" + CONTRACT_OFFER)

    @Override
    public void providerOffer(@PathParam("id") String id, @RequestBody(description = DSPACE_NEGOTIATION_CONTRACT_OFFER_MESSAGE, required = true) JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractOfferMessage for process %s", id));

        checkAndReturnAuthToken(token);

        throw new UnsupportedOperationException("Processing of dspace:ContractOfferMessage currently not supported.");
    }

    @POST
    @Path("{id}" + AGREEMENT)
    @Override
    public void createAgreement(@PathParam("id") String id, @RequestBody(description = DSPACE_NEGOTIATION_AGREEMENT_MESSAGE, required = true) JsonObject body, @HeaderParam(AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractAgreementMessage for process %s", id));

        var claimToken = checkAndReturnAuthToken(token);

        var expanded = expand(body).getJsonObject(0);
        var message = transformerRegistry.transform(hasValidType(expanded, DSPACE_NEGOTIATION_AGREEMENT_MESSAGE), ContractAgreementMessage.class)
                .orElseThrow(failure -> new InvalidRequestException(format("Request body was malformed: %s", failure.getFailureDetail())));

        validateId(message.getProcessId(), id);

        protocolService.notifyAgreed(message, claimToken).orElseThrow(exceptionMapper(ContractNegotiation.class));
    }

    private ClaimToken checkAndReturnAuthToken(String token) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();

        var result = identityService.verifyJwtToken(tokenRepresentation, callbackAddress);
        if (result.failed()) {
            throw new AuthenticationFailedException();
        }

        return result.getContent();
    }

    private JsonObject hasValidType(JsonObject object, String expected) {
        if (isOfExpectedType(object, expected)) {
            return object;
        } else {
            throw new InvalidRequestException(format("Request body was not of expected type: %s", expected));
        }
    }

    private void validateId(String actual, String expected) {
        if (!actual.equals(expected)) {
            throw new InvalidRequestException(String.format("ProcessId %s is not matching path id %s.", actual, expected));
        }
    }

    // TODO refactor according to https://github.com/eclipse-edc/Connector/issues/2763
    private JsonObject jsonLdContext() {
        return Json.createObjectBuilder()
                .add(ODRL_PREFIX, ODRL_SCHEMA)
                .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                .build();
    }
}
