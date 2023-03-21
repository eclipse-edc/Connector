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

package org.eclipse.edc.protocol.dsp.controlplane.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.ProviderContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.controlplane.service.DspContractNegotiationService;
import org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationError;
import org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationEventType;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.List;

import static org.eclipse.edc.protocol.dsp.transform.util.DocumentUtil.compactDocument;

public class ContractNegotiationServiceImpl implements DspContractNegotiationService {

    private final ContractNegotiationService contractNegotiationService;

    private final JsonLdTransformerRegistry registry;

    private final ObjectMapper mapper;

    // TODO remove managers as soon as core ContractNegotiationServiceImpl can handle all transitions
    private final ConsumerContractNegotiationManager consumerNegotiationManager;

    private final ProviderContractNegotiationManager providerNegotiationManager;

    public ContractNegotiationServiceImpl(ContractNegotiationService contractNegotiationService,
                                          JsonLdTransformerRegistry registry, ObjectMapper mapper,
                                          ConsumerContractNegotiationManager consumerNegotiationManager,
                                          ProviderContractNegotiationManager providerNegotiationManager) {
        this.contractNegotiationService = contractNegotiationService;
        this.registry = registry;
        this.mapper = mapper;

        this.consumerNegotiationManager = consumerNegotiationManager;
        this.providerNegotiationManager = providerNegotiationManager;
    }

    @Override
    public JsonObject getNegotiationById(String id) {
        var negotiation = contractNegotiationService.findbyId(id);

        if (negotiation == null) {
            throw new ObjectNotFoundException(ContractNegotiation.class, id);
        }

        var result = registry.transform(negotiation, JsonObject.class);
        if (result.failed()) {
            throw new EdcException("Response could not be created.");
        }

        return result.getContent();
    }

    @Override
    public JsonObject createNegotiation(JsonObject message) {
        // TODO add validation of request id with process id in message
        var resultRequest = registry.transform(message, ContractOfferRequest.class);
        if (resultRequest.failed()) {
            throw new InvalidRequestException("Request body was malformed.");
        }

        var negotiation = contractNegotiationService.initiateNegotiation(resultRequest.getContent());

        var resultNegotiation = registry.transform(negotiation, JsonObject.class);
        if (resultRequest.failed()) {
            throw new EdcException("Response could not be created.");
        }

        return resultNegotiation.getContent();
    }

    @Override
    public void consumerOffer(String id, JsonObject message) {
        // TODO support additional offers by consumer/provider
        throw new EdcException("Additional offers by consumer cannot be processed.");
    }

    @Override
    public void processEvent(String id, JsonObject message) {
        String eventType;
        try {
            eventType = message.getString("dspace:eventType");
        } catch (NullPointerException e) {
            throw new InvalidRequestException("Value dspace:eventType is missing or malformed.");
        }

        switch (eventType) {
            case ContractNegotiationEventType.FINALIZED:
                finalizeAgreement(id);
                break;
            case ContractNegotiationEventType.ACCEPTED:
                acceptCurrentOffer(id);
                break;
            default:
                throw new InvalidRequestException(String.format("Cannot process dspace:ContractNegotiationEventMessage with unexpected type: %s.",
                        eventType));
        }
    }

    @Override
    public void verifyAgreement(String id, JsonObject message) {
        // TODO after https://github.com/eclipse-edc/Connector/pull/2601
    }

    @Override
    public void terminateNegotiation(String id, JsonObject message) {
        var processId = getValueByKey(message, "dspace:processId");

        var result = contractNegotiationService.cancel(processId); // TODO or decline?
        if (result.failed()) {
            throw new EdcException(String.format("Failed to cancel contract negotiation with id %s.", processId));
        }
    }

    @Override
    public void providerOffer(String id, JsonObject message) {
        // TODO allow (counter) offers by provider
        throw new EdcException("Consumer cannot handle (counter) offers by provider.");
    }

    @Override
    public void createAgreement(String id, JsonObject message) {
        var processId = getValueByKey(message, "dspace:processId");
        var agreement = getObjectByKey(message, "dspace:agreement");

        var resultAgreement = registry.transform(agreement, ContractAgreement.class); //TODO transformer
        if (resultAgreement.failed()) {
            throw new InvalidRequestException("Agreement could not be processed.");
        }

        // NOTE: claim token and policy not used in manager, why is it forwarded? TODO
        consumerNegotiationManager.confirmed(null, processId, resultAgreement.getContent(), null);
    }

    @Override
    public void acceptCurrentOffer(String id) {
        // NOTE: consumer cannot accept offer as (counter) offers by provider are not supported
        throw new EdcException("Failed to process dspace:ContractNegotiationEventMessage with status ACCEPTED.");
    }

    @Override
    public void finalizeAgreement(String id) {
        // TODO after https://github.com/eclipse-edc/Connector/pull/2601
        // Error: "Failed to process dspace:ContractNegotiationEventMessage with status FINALIZED."
    }

    /**
     * Build a negotiation error that can be used as response body for codes 4xx and 5xx.
     * TODO support ContractNegotiationError
     *
     * @param code      response code.
     * @param processId negotiation process id.
     * @param reason    why the request could not be processed.
     * @return a dspace:ContractNegotiationError as String.
     */
    private String createNegotiationError(String code, String processId, String reason) {
        var error = ContractNegotiationError.Builder.newInstance()
                .code(code)
                .processId(processId)
                .reasons(List.of(reason))
                .build();

        var result = registry.transform(error, JsonObject.class);

        return mapper.convertValue(compactDocument(result.getContent()), JsonObject.class).toString();
    }

    private String getValueByKey(JsonObject object, String key) {
        try {
            return object.get(key).toString();
        } catch (NullPointerException e) {
            throw new InvalidRequestException(String.format("Value %s is missing.", key));
        }
    }

    private JsonObject getObjectByKey(JsonObject object, String key) {
        try {
            return object.get(key).asJsonObject();
        } catch (NullPointerException e) {
            throw new InvalidRequestException(String.format("Value %s is missing.", key));
        }
    }
}
