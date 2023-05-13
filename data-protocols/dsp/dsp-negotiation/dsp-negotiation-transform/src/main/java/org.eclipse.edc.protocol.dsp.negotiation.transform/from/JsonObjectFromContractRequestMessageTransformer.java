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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_DATASET;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_OFFER_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;


/**
 * Creates a {@link JsonObject} from a {@link ContractRequestMessage}.
 */
public class JsonObjectFromContractRequestMessageTransformer extends AbstractJsonLdTransformer<ContractRequestMessage, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractRequestMessageTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractRequestMessage.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractRequestMessage requestMessage, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(JsonLdKeywords.ID, requestMessage.getId());
        builder.add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE);

        builder.add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, requestMessage.getProcessId());

        if (requestMessage.getCallbackAddress() != null) {
            builder.add(DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS, requestMessage.getCallbackAddress());
        }

        if (requestMessage.getContractOffer() != null) {
            builder.add(DSPACE_NEGOTIATION_PROPERTY_DATASET, requestMessage.getContractOffer().getAssetId());
            var policy = context.transform(requestMessage.getContractOffer().getPolicy(), JsonObject.class);
            if (policy == null) {
                context.reportProblem("Cannot transform from ContractRequestMessage policy");
                return null;
            }
            builder.add(DSPACE_NEGOTIATION_PROPERTY_OFFER, policy);

        } else {
            builder.add(DSPACE_NEGOTIATION_PROPERTY_OFFER_ID, requestMessage.getContractOfferId());
            if (requestMessage.getDataSet() != null) {
                builder.add(DSPACE_NEGOTIATION_PROPERTY_DATASET, requestMessage.getDataSet());
            }
        }

        return builder.build();
    }

}
