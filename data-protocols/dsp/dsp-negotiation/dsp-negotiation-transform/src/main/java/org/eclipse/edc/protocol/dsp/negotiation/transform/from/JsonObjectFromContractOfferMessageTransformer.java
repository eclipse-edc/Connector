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

package org.eclipse.edc.protocol.dsp.negotiation.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_OFFER;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_OFFER_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROCESS_ID;

/**
 * Creates a {@link JsonObject} from a {@link ContractOfferMessage}.
 */
public class JsonObjectFromContractOfferMessageTransformer extends AbstractJsonLdTransformer<ContractOfferMessage, JsonObject> {
    
    private final JsonBuilderFactory jsonFactory;
    
    public JsonObjectFromContractOfferMessageTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractOfferMessage.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }
    
    @Override
    public @Nullable JsonObject transform(@NotNull ContractOfferMessage message, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(ID, message.getId());
        builder.add(TYPE, DSPACE_TYPE_CONTRACT_OFFER_MESSAGE);
        builder.add(DSPACE_PROPERTY_PROCESS_ID, message.getProcessId());
    
        if (message.getCallbackAddress() != null) {
            builder.add(DSPACE_PROPERTY_CALLBACK_ADDRESS, message.getCallbackAddress());
        }
        
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
        builder.add(DSPACE_PROPERTY_OFFER, enrichedPolicy);
        
        return builder.build();
    }
}
