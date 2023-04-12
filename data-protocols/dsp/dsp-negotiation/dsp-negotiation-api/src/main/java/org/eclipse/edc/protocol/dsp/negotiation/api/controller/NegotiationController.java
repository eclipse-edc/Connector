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
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.eclipse.edc.jsonld.JsonLdExtension.TYPE_MANAGER_CONTEXT_JSON_LD;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.compact;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_ACCEPTED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_FINALIZED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.CONTRACT_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.EVENT;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.TERMINATION;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.VERIFICATION;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.ODRL_PREFIX;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.ODRL_SCHEMA;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path(BASE_PATH)
public class NegotiationController implements NegotiationApi {

    private final IdentityService identityService;
    private final JsonLdTransformerRegistry transformerRegistry;
    private final String callbackAddress;
    private final Monitor monitor;
    private final ContractNegotiationService service;
    private final ContractNegotiationProtocolService protocolService;
    private final ObjectMapper mapper;

    public NegotiationController(Monitor monitor, TypeManager typeManager, String callbackAddress,
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
    public JsonObject getNegotiation(@PathParam("id") String id, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming request for contract negotiation with id %s", id));

        checkAndReturnAuthToken(token);

        var negotiation = service.findbyId(id);
        if (negotiation == null) {
            throw new ObjectNotFoundException(ContractNegotiation.class, id);
        }

        var result = transformerRegistry.transform(negotiation, JsonObject.class);
        if (result.failed()) {
            throw new EdcException(format("Failed to build response: %s", join(", ", result.getFailureMessages())));
        }

        return mapper.convertValue(compact(result.getContent(), jsonLdContext()), JsonObject.class);
    }

    @POST
    @Path(INITIAL_CONTRACT_REQUEST)
    @Override
    public JsonObject initiateNegotiation(@RequestBody(description = "dspace:ContractRequestMessage", required = true) JsonObject body, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug("DSP: Incoming ContractRequestMessage for initiating a contract negotiation.");

        var claimToken = checkAndReturnAuthToken(token);

        var expanded = expand(body).getJsonObject(0);
        var message = (ContractRequestMessage) transformRequestToMessage(expanded, ContractRequestMessage.class);
        var negotiation = protocolService.notifyRequested(message, claimToken);
        if (negotiation.failed()) {
            throw new EdcException(format("Failed to process dspace:ContractRequestMessage: %s", join(", ", negotiation.getFailureMessages())));
        }

        var result = transformerRegistry.transform(negotiation.getContent(), JsonObject.class);
        if (result.failed() || result.getContent() == null) {
            throw new EdcException(format("Failed to build response: %s", join(", ", result.getFailureMessages())));
        }

        return mapper.convertValue(compact(result.getContent(), jsonLdContext()), JsonObject.class);
    }

    @POST
    @Path("{id}" + CONTRACT_REQUEST)
    @Override
    public void consumerOffer(@PathParam("id") String id, @RequestBody(description = "dspace:ContractRequestMessage", required = true) JsonObject body, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractRequestMessage for process %s", id));

        var claimToken = checkAndReturnAuthToken(token);
        var expanded = expand(body).getJsonObject(0);

        validateId(expanded, id);

        var message = (ContractRequestMessage) transformRequestToMessage(expanded, ContractRequestMessage.class); // TODO write transformer
        var result = protocolService.notifyRequested(message, claimToken);
        if (result.failed()) {
            throw new EdcException(format("Failed to process dspace:ContractRequestMessage: %s", join(", ", result.getFailureMessages())));
        }
    }

    @POST
    @Path("{id}" + EVENT)
    @Override
    public void createEvent(@PathParam("id") String id, @RequestBody(description = "dspace:ContractNegotiationEventMessage", required = true) JsonObject body, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractNegotiationEventMessage for process %s", id));

        var claimToken = checkAndReturnAuthToken(token);
        var expanded = expand(body).getJsonObject(0);

        validateId(expanded, id);

        var message = (ContractNegotiationEventMessage) transformRequestToMessage(expanded, ContractNegotiationEventMessage.class); // TODO write transformer
        var eventType = getValueByKey(expanded, DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE);
        switch (eventType) {
            case DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_FINALIZED:
                var resultFinalized = protocolService.notifyFinalized(message, claimToken);
                if (resultFinalized.failed()) {
                    throw new EdcException(format("Failed to process dspace:ContractNegotiationEventMessage with status %s: %s", DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_FINALIZED, join(", ", resultFinalized.getFailureMessages())));
                }
                break;
            case DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_ACCEPTED:
                var resultAccepted = protocolService.notifyAccepted(message, claimToken);
                if (resultAccepted.failed()) {
                    throw new EdcException(format("Failed to process dspace:ContractNegotiationEventMessage with status %s: %s", DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_ACCEPTED, join(", ", resultAccepted.getFailureMessages())));
                }
                break;
            default:
                throw new InvalidRequestException(String.format("Cannot process dspace:ContractNegotiationEventMessage with unexpected type %s.", eventType));
        }
    }

    @POST
    @Path("{id}" + AGREEMENT + VERIFICATION)
    @Override
    public void verifyAgreement(@PathParam("id") String id, @RequestBody(description = "dspace:ContractAgreementVerificationMessage", required = true) JsonObject body, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractAgreementVerificationMessage for process %s", id));

        var claimToken = checkAndReturnAuthToken(token);
        var expanded = expand(body).getJsonObject(0);

        validateId(expanded, id);

        var message = (ContractAgreementVerificationMessage) transformRequestToMessage(expanded, ContractAgreementVerificationMessage.class); // TODO write transformer
        var result = protocolService.notifyVerified(message, claimToken);
        if (result.failed()) {
            throw new EdcException(format("Failed to process dspace:ContractAgreementVerificationMessage: %s", join(", ", result.getFailureMessages())));
        }
    }

    @POST
    @Path("{id}" + TERMINATION)
    @Override
    public void terminateNegotiation(@PathParam("id") String id, @RequestBody(description = "dspace:ContractNegotiationTerminationMessage", required = true) JsonObject body, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractNegotiationTerminationMessage for process %s", id));

        var claimToken = checkAndReturnAuthToken(token);
        var expanded = expand(body).getJsonObject(0);

        validateId(expanded, id);

        var message = (ContractNegotiationTerminationMessage) transformRequestToMessage(expanded, ContractNegotiationTerminationMessage.class); // TODO write transformer
        var result = protocolService.notifyTerminated(message, claimToken); // TODO or decline?
        if (result.failed()) {
            throw new EdcException(format("Failed to cancel contract negotiation with id %s: %s", id, join(", ", result.getFailureMessages())));
        }
    }

    @POST
    @Path("{id}" + CONTRACT_OFFER)

    @Override
    public void providerOffer(@PathParam("id") String id, @RequestBody(description = "dspace:ContractOfferMessage", required = true) JsonObject body, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractOfferMessage for process %s", id));

        var claimToken = checkAndReturnAuthToken(token);
        var expanded = expand(body).getJsonObject(0);

        validateId(expanded, id);

        var message = (ContractRequestMessage) transformRequestToMessage(expanded, ContractRequestMessage.class); // TODO write transformer
        var result = protocolService.notifyOffered(message, claimToken);
        if (result.failed()) {
            throw new EdcException(format("Failed to process dspace:ContractOfferMessage: %s", join(", ", result.getFailureMessages())));
        }
    }

    @POST
    @Path("{id}" + AGREEMENT)
    @Override
    public void createAgreement(@PathParam("id") String id, @RequestBody(description = "dspace:ContractAgreementMessage", required = true) JsonObject body, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        monitor.debug(format("DSP: Incoming ContractAgreementMessage for process %s", id));

        var claimToken = checkAndReturnAuthToken(token);
        var expanded = expand(body).getJsonObject(0);

        validateId(expanded, id);

        var message = (ContractAgreementMessage) transformRequestToMessage(expanded, ContractAgreementMessage.class); // TODO write transformer
        var result = protocolService.notifyAgreed(message, claimToken);
        if (result.failed()) {
            throw new EdcException(format("Failed to process dspace:ContractAgreementMessage: %s", join(", ", result.getFailureMessages())));
        }
    }

    private ClaimToken checkAndReturnAuthToken(String token) { // TODO refactor: move to dsp core module?
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();

        var result = identityService.verifyJwtToken(tokenRepresentation, callbackAddress);
        if (result.failed()) {
            throw new AuthenticationFailedException();
        }

        return result.getContent();
    }

    private String getValueByKey(JsonObject object, String key) {
        try {
            return object.getJsonString(key).getString();
        } catch (NullPointerException e) {
            throw new InvalidRequestException(String.format("Value %s is missing.", key));
        }
    }

    private void validateId(JsonObject message, String requestId) {
        var processId = getValueByKey(message, DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID);
        if (!requestId.equals(processId)) {
            throw new InvalidRequestException(String.format("ProcessId %s is not matching path id %s.", processId, requestId));
        }
    }

    private RemoteMessage transformRequestToMessage(JsonObject message, Class<? extends RemoteMessage> type) {
        var resultRequest = transformerRegistry.transform(message, type);
        if (resultRequest.failed()) {
            throw new InvalidRequestException(format("Request body was malformed: %s", join(", ", resultRequest.getFailureMessages())));
        }

        return resultRequest.getContent();
    }

    private JsonObject jsonLdContext() {
        return Json.createObjectBuilder()
                .add(DCAT_PREFIX, DCAT_SCHEMA)
                .add(DCT_PREFIX, DCT_SCHEMA)
                .add(ODRL_PREFIX, ODRL_SCHEMA)
                .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                .build();
    }
}
