/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.transform.to;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Set;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_AGREEMENT_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_ID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_ID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_TIMESTAMP_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;

/**
 * Creates a {@link ContractAgreementMessage} from a {@link JsonObject}.
 */
public class JsonObjectToContractAgreementMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, ContractAgreementMessage> {
    private final Set<String> excludedPolicyKeywords;

    public JsonObjectToContractAgreementMessageTransformer(JsonLdNamespace namespace) {
        super(JsonObject.class, ContractAgreementMessage.class, namespace);
        excludedPolicyKeywords = Set.of(forNamespace(DSPACE_PROPERTY_TIMESTAMP_TERM),
                forNamespace(DSPACE_PROPERTY_CONSUMER_ID_TERM),
                forNamespace(DSPACE_PROPERTY_PROVIDER_ID_TERM));
    }

    @Override
    public @Nullable ContractAgreementMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var messageBuilder = ContractAgreementMessage.Builder.newInstance();
        if (!transformMandatoryString(object.get(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM)), messageBuilder::consumerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM))
                    .report();
            return null;
        }
        if (!transformMandatoryString(object.get(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM)), messageBuilder::providerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM))
                    .report();
            return null;
        }

        var jsonAgreement = returnMandatoryJsonObject(object.get(forNamespace(DSPACE_PROPERTY_AGREEMENT_TERM)), context, forNamespace(DSPACE_PROPERTY_AGREEMENT_TERM));
        if (jsonAgreement == null) {
            return null;
        }

        var filteredJsonAgreement = filterAgreementProperties(jsonAgreement);

        var policy = context.transform(filteredJsonAgreement, Policy.class);
        if (policy == null) {
            context.problem()
                    .invalidProperty()
                    .type(forNamespace(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_AGREEMENT_TERM))
                    .report();
            return null;
        }

        var agreement = contractAgreement(jsonAgreement, policy, context);
        if (agreement == null) {
            // problem already reported
            return null;
        }

        var callbackAddress = transformString(jsonAgreement.get(forNamespace(DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM)), context);
        messageBuilder.callbackAddress(callbackAddress);

        messageBuilder.contractAgreement(agreement);

        return messageBuilder.build();
    }

    private JsonObject filterAgreementProperties(JsonObject jsonAgreement) {
        var copiedJsonAgreement = Json.createObjectBuilder();
        jsonAgreement.entrySet().stream()
                .filter(e -> !excludedPolicyKeywords.contains(e.getKey()))
                .forEach(e -> copiedJsonAgreement.add(e.getKey(), e.getValue()));
        return copiedJsonAgreement.build();
    }

    @Nullable
    private ContractAgreement contractAgreement(JsonObject jsonAgreement, Policy policy, TransformerContext context) {
        var builder = ContractAgreement.Builder.newInstance();
        var agreementId = nodeId(jsonAgreement);
        if (agreementId == null) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM))
                    .property(ID)
                    .report();
            return null;
        }

        var assignee = policy.getAssignee();
        var assigner = policy.getAssigner();

        builder.agreementId(agreementId)
                .consumerId(assignee)
                .providerId(assigner)
                .policy(policy)
                .assetId(policy.getTarget());

        var timestamp = transformString(jsonAgreement.get(forNamespace(DSPACE_PROPERTY_TIMESTAMP_TERM)), context);
        if (timestamp == null) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_TIMESTAMP_TERM))
                    .report();
            return null;
        }
        try {
            builder.contractSigningDate(Instant.parse(timestamp).getEpochSecond());
        } catch (DateTimeParseException e) {
            context.problem()
                    .invalidProperty()
                    .type(forNamespace(DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_TIMESTAMP_TERM))
                    .value(timestamp)
                    .error(e.getMessage())
                    .report();
            return null;
        }

        return builder.build();
    }
}
