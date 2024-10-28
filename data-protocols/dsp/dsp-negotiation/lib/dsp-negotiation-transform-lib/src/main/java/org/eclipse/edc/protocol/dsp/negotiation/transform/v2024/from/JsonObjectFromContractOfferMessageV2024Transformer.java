/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.transform.v2024.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_OFFER_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;

/**
 * Creates a {@link JsonObject} from a {@link ContractOfferMessage}.
 */
public class JsonObjectFromContractOfferMessageV2024Transformer extends AbstractNamespaceAwareJsonLdTransformer<ContractOfferMessage, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractOfferMessageV2024Transformer(JsonBuilderFactory jsonFactory) {
        this(jsonFactory, DSP_NAMESPACE_V_2024_1);
    }

    public JsonObjectFromContractOfferMessageV2024Transformer(JsonBuilderFactory jsonFactory, JsonLdNamespace namespace) {
        super(ContractOfferMessage.class, JsonObject.class, namespace);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractOfferMessage message, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(ID, message.getId())
                .add(TYPE, forNamespace(DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM))
                .add(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM), createId(jsonFactory, message.getProviderPid()));

        addIdIfNotNull(message.getConsumerPid(), forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM), jsonFactory, builder);
        addIfNotNull(message.getCallbackAddress(), forNamespace(DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM), builder);

        var offer = message.getContractOffer();
        var policy = context.transform(offer.getPolicy(), JsonObject.class);
        if (policy == null) {
            context.problem()
                    .nullProperty()
                    .type(ContractOfferMessage.class)
                    .property("contractOffer")
                    .report();
            return null;
        }
        var enrichedPolicy = Json.createObjectBuilder(policy)
                .add(ID, offer.getId())
                .build();
        builder.add(forNamespace(DSPACE_PROPERTY_OFFER_TERM), enrichedPolicy);

        return builder.build();
    }
}
