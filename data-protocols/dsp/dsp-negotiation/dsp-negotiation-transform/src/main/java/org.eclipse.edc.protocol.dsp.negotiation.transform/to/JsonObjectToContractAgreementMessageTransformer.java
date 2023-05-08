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

package org.eclipse.edc.protocol.dsp.negotiation.transform.to;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CONSUMER_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROVIDER_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_TIMESTAMP;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;

/**
 * Creates a {@link ContractAgreementMessage} from a {@link JsonObject}.
 */
public class JsonObjectToContractAgreementMessageTransformer extends AbstractJsonLdTransformer<JsonObject, ContractAgreementMessage> {
    private static final Set<String> EXCLUDED_POLICY_KEYWORDS =
            Set.of(DSPACE_NEGOTIATION_PROPERTY_CONSUMER_ID, DSPACE_NEGOTIATION_PROPERTY_PROVIDER_ID);

    public JsonObjectToContractAgreementMessageTransformer() {
        super(JsonObject.class, ContractAgreementMessage.class);
    }

    @Override
    public @Nullable ContractAgreementMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var messageBuilder = ContractAgreementMessage.Builder.newInstance();
        messageBuilder.protocol(DATASPACE_PROTOCOL_HTTP);
        transformString(object.get(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID), messageBuilder::processId, context);

        var jsonAgreement = object.getJsonObject(DSPACE_NEGOTIATION_PROPERTY_AGREEMENT);
        if (jsonAgreement == null) {
            context.reportProblem("Cannot transform to ContractAgreementMessage with null agreement");
            return null;
        }

        var filteredJsonAgreement = filterAgreementProperties(jsonAgreement);

        var policy = context.transform(filteredJsonAgreement, Policy.class);
        if (policy == null) {
            context.reportProblem("Cannot transform to ContractAgreementMessage with invalid policy");
            return null;
        }

        var agreement = contractAgreement(object, jsonAgreement, policy, context);
        if (agreement == null) {
            context.reportProblem("Cannot transform to ContractAgreementMessage with null agreement");
            return null;
        }

        messageBuilder.contractAgreement(agreement);

        return messageBuilder.build();
    }

    private JsonObject filterAgreementProperties(JsonObject jsonAgreement) {
        var copiedJsonAgreement = Json.createObjectBuilder();
        jsonAgreement.entrySet().stream()
                .filter(e -> !EXCLUDED_POLICY_KEYWORDS.contains(e.getKey()))
                .forEach(e -> copiedJsonAgreement.add(e.getKey(), e.getValue()));
        return copiedJsonAgreement.build();
    }

    @Nullable
    private ContractAgreement contractAgreement(JsonObject jsonMessage, JsonObject jsonAgreement, Policy policy, TransformerContext context) {
        var builder = ContractAgreement.Builder.newInstance();
        var agreementId = nodeId(jsonAgreement);
        if (agreementId == null) {
            context.reportProblem("No id specified on ContractAgreement");
            return null;
        }
        builder.id(agreementId);

        var consumerId = jsonAgreement.get(DSPACE_NEGOTIATION_PROPERTY_CONSUMER_ID);
        if (consumerId == null) {
            context.reportProblem("No consumer id specified on ContractAgreement");
            return null;
        }
        transformString(consumerId, builder::consumerId, context);

        var providerId = jsonAgreement.get(DSPACE_NEGOTIATION_PROPERTY_PROVIDER_ID);
        if (providerId == null) {
            context.reportProblem("No provider id specified on ContractAgreement");
            return null;
        }
        transformString(providerId, builder::providerId, context);

        builder.policy(policy);
        builder.assetId(policy.getTarget());

        var timestamp = jsonMessage.getString(DSPACE_NEGOTIATION_PROPERTY_TIMESTAMP);
        try {
            builder.contractSigningDate(Long.parseLong(timestamp));
        } catch (NumberFormatException exception) {
            context.reportProblem(String.format("Cannot transform %s to long in ContractAgreementMessage", timestamp));
            return null;
        }

        return builder.build();
    }
}
