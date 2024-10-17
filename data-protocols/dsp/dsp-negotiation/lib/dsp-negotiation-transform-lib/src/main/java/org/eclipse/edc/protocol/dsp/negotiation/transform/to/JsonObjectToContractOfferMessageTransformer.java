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

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_OFFER_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;

/**
 * Creates a {@link ContractOfferMessage} from a {@link JsonObject}.
 */
public class JsonObjectToContractOfferMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, ContractOfferMessage> {

    public JsonObjectToContractOfferMessageTransformer() {
        this(DSPACE_SCHEMA);
    }

    public JsonObjectToContractOfferMessageTransformer(String namespace) {
        super(JsonObject.class, ContractOfferMessage.class, namespace);
    }

    @Override
    public @Nullable ContractOfferMessage transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = ContractOfferMessage.Builder.newInstance();
        if (!transformMandatoryString(jsonObject.get(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM)), builder::providerPid, context)) {
            reportMissingProperty(forNamespace(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM), forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM), context);
            return null;
        }

        var consumerPid = jsonObject.get(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM));
        if (consumerPid != null) {
            builder.consumerPid(transformString(consumerPid, context));
        }

        var callback = jsonObject.get(forNamespace(DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM));
        if (callback != null) {
            builder.callbackAddress(transformString(callback, context));
        }

        var contractOffer = returnJsonObject(jsonObject.get(forNamespace(DSPACE_PROPERTY_OFFER_TERM)), context, forNamespace(DSPACE_PROPERTY_OFFER_TERM), false);
        if (contractOffer != null) {
            context.setData(Policy.class, TYPE, PolicyType.OFFER);
            var policy = transformObject(contractOffer, Policy.class, context);
            if (policy == null) {
                reportMissingProperty(forNamespace(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM), forNamespace(DSPACE_PROPERTY_OFFER_TERM), context);
                return null;
            }
            var id = nodeId(contractOffer);
            if (id == null) {
                reportMissingProperty(forNamespace(DSPACE_PROPERTY_OFFER_TERM), ID, context);
                return null;
            }
            var offer = ContractOffer.Builder.newInstance()
                    .id(id)
                    .assetId(policy.getTarget())
                    .policy(policy)
                    .build();
            builder.contractOffer(offer);
        } else {
            reportMissingProperty(forNamespace(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM), forNamespace(DSPACE_PROPERTY_OFFER_TERM), context);
            return null;
        }

        return builder.build();
    }

    private void reportMissingProperty(String type, String property, TransformerContext context) {
        context.problem()
                .missingProperty()
                .type(type)
                .property(property)
                .report();
    }
}
