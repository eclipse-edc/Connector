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

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Set;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CONSUMER_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROVIDER_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_TIMESTAMP;

/**
 * Creates a {@link ContractAgreementMessage} from a {@link JsonObject}.
 */
public class JsonObjectToContractAgreementMessageTransformer extends AbstractJsonLdTransformer<JsonObject, ContractAgreementMessage> {
    private static final Set<String> EXCLUDED_POLICY_KEYWORDS =
            Set.of(DSPACE_NEGOTIATION_PROPERTY_CONSUMER_ID, DSPACE_NEGOTIATION_PROPERTY_PROVIDER_ID, DSPACE_NEGOTIATION_PROPERTY_TIMESTAMP);

    public JsonObjectToContractAgreementMessageTransformer() {
        super(JsonObject.class, ContractAgreementMessage.class);
    }

    @Override
    public @Nullable ContractAgreementMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var messageBuilder = ContractAgreementMessage.Builder.newInstance();
        if (!transformMandatoryString(object.get(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID), messageBuilder::processId, context)) {
            context.reportProblem(format("No '%s' specified on ContractAgreementMessage", DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID));
            return null;
        }

        var jsonAgreement = returnMandatoryJsonObject(object.get(DSPACE_NEGOTIATION_PROPERTY_AGREEMENT), context, DSPACE_NEGOTIATION_PROPERTY_AGREEMENT);
        if (jsonAgreement == null) {
            return null;
        }

        var filteredJsonAgreement = filterAgreementProperties(jsonAgreement);

        var policy = context.transform(filteredJsonAgreement, Policy.class);
        if (policy == null) {
            context.reportProblem("Cannot transform to ContractAgreementMessage with invalid policy");
            return null;
        }

        var agreement = contractAgreement(jsonAgreement, policy, context);
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
    private ContractAgreement contractAgreement(JsonObject jsonAgreement, Policy policy, TransformerContext context) {
        var builder = ContractAgreement.Builder.newInstance();
        var agreementId = nodeId(jsonAgreement);
        if (agreementId == null) {
            context.reportProblem("No id specified on ContractAgreement");
            return null;
        }
        builder.id(agreementId);

        if (!transformMandatoryString(jsonAgreement.get(DSPACE_NEGOTIATION_PROPERTY_CONSUMER_ID), builder::consumerId, context)) {
            context.reportProblem(format("No '%s' specified on ContractAgreement", DSPACE_NEGOTIATION_PROPERTY_CONSUMER_ID));
            return null;
        }

        if (!transformMandatoryString(jsonAgreement.get(DSPACE_NEGOTIATION_PROPERTY_PROVIDER_ID), builder::providerId, context)) {
            context.reportProblem(format("No '%s' specified on ContractAgreement", DSPACE_NEGOTIATION_PROPERTY_PROVIDER_ID));
            return null;
        }

        builder.policy(policy);
        builder.assetId(policy.getTarget());

        var timestamp = transformString(jsonAgreement.get(DSPACE_NEGOTIATION_PROPERTY_TIMESTAMP), context);
        if (timestamp == null) {
            context.reportProblem(format("No '%s' specified on ContractAgreement", DSPACE_NEGOTIATION_PROPERTY_TIMESTAMP));
            return null;
        }
        try {
            builder.contractSigningDate(Instant.parse(timestamp).getEpochSecond());
        } catch (DateTimeParseException e) {
            context.reportProblem(format("Invalid '%s' specified on ContractAgreement: %s", DSPACE_NEGOTIATION_PROPERTY_TIMESTAMP, e.getMessage()));
            return null;
        }

        return builder.build();
    }
}
