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
            var error = "Response could not be created.";
            throw new EdcException(createNegotiationError("500", id, error));
        }

        return result.getContent();
    }

    @Override
    public JsonObject createNegotiation(JsonObject message) {
        var resultRequest = registry.transform(message, ContractOfferRequest.class);
        if (resultRequest.failed()) {
            var error = "Request body was malformed.";
            throw new InvalidRequestException(createNegotiationError("400", null, error));
        }

        var negotiation = contractNegotiationService.initiateNegotiation(resultRequest.getContent());

        var resultNegotiation = registry.transform(negotiation, JsonObject.class);
        if (resultRequest.failed()) {
            var error = "Response could not be created.";
            throw new EdcException(createNegotiationError("500", null, error));
        }

        return resultNegotiation.getContent();
    }

    @Override
    public void consumerOffer(String id, JsonObject message) {
        validateId(message, id);

        // TODO support additional offers by consumer/provider (not initiate)
        var error = "Additional offers by consumer cannot be processed.";
        throw new EdcException(createNegotiationError("500", id, error));
    }

    @Override
    public void processEvent(String id, JsonObject message) {
        validateId(message, id);

        var eventType = getValueByKey(message, "dspace:eventType");
        switch (eventType) {
            case ContractNegotiationEventType.FINALIZED:
                finalizeAgreement(id);
                break;
            case ContractNegotiationEventType.ACCEPTED:
                acceptCurrentOffer(id);
                break;
            default:
                var error = String.format("Cannot process dspace:ContractNegotiationEventMessage with unexpected type %s.", eventType);
                throw new InvalidRequestException(createNegotiationError("400", id, error));
        }
    }

    @Override
    public void verifyAgreement(String id, JsonObject message) {
        validateId(message, id);

        var result = providerNegotiationManager.verified(id);
        if (result.failed()) {
            var error = "Failed to process dspace:ContractAgreementVerificationMessage.";
            throw new EdcException(createNegotiationError("500", id, error));
        }
    }

    @Override
    public void terminateNegotiation(String id, JsonObject message) {
        validateId(message, id);

        var result = contractNegotiationService.cancel(id); // TODO or decline?
        if (result.failed()) {
            var error = String.format(String.format("Failed to cancel contract negotiation with id %s.", id));
            throw new EdcException(createNegotiationError("500", id, error));
        }
    }

    @Override
    public void providerOffer(String id, JsonObject message) {
        validateId(message, id);

        // TODO allow (counter) offers by provider
        var error = "Consumer cannot handle (counter) offers by provider.";
        throw new EdcException(createNegotiationError("500", id, error));
    }

    @Override
    public void createAgreement(String id, JsonObject message) {
        validateId(message, id);

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
        var error = "Failed to process dspace:ContractNegotiationEventMessage with status ACCEPTED.";
        throw new EdcException(createNegotiationError("500", id, error));
    }

    @Override
    public void finalizeAgreement(String id) {
        var result = consumerNegotiationManager.finalized(id);
        if (result.failed()) {
            var error = "Failed to process dspace:ContractNegotiationEventMessage with status FINALIZED.";
            throw new EdcException(createNegotiationError("500", id, error));
        }
    }

    /**
     * Build a negotiation error that can be used as response body for codes 4xx and 5xx.
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

    private void validateId(JsonObject message, String requestId) {
        var processId = getValueByKey(message, "dspace:processId");
        if (!requestId.equals(processId)) {
            throw new InvalidRequestException(String.format("ProcessId %s is not matching the requestId %s.", processId, requestId));
        }
    }
}
