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
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage.Type.INITIAL;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_DATASET;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;

/**
 * Creates a {@link ContractRequestMessage} from a {@link JsonObject}.
 */
public class JsonObjectToContractRequestMessageTransformer extends AbstractJsonLdTransformer<JsonObject, ContractRequestMessage> {

    public JsonObjectToContractRequestMessageTransformer() {
        super(JsonObject.class, ContractRequestMessage.class);
    }

    @Override
    public @Nullable ContractRequestMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = ContractRequestMessage.Builder.newInstance()
                .protocol(DATASPACE_PROTOCOL_HTTP)
                .type(INITIAL);

        transformString(object.get(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID), builder::processId, context);
        transformString(object.get(DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS), builder::callbackAddress, context);
        transformString(object.get(DSPACE_NEGOTIATION_PROPERTY_DATASET), builder::dataSet, context);

        var policy = transformPolicy(object.getJsonObject(DSPACE_NEGOTIATION_PROPERTY_OFFER), context);
        if (policy == null) {
            context.reportProblem("Cannot transform to ContractRequestMessage with null policy");
            return null;
        }

        builder.contractOffer(contractOffer(object, policy));

        return builder.build();
    }

    private @Nullable Policy transformPolicy(JsonObject policy, TransformerContext context) {
        return context.transform(policy, Policy.class);
    }

    private ContractOffer contractOffer(JsonObject offer, Policy policy) {
        var builder = ContractOffer.Builder.newInstance();
        builder.id(nodeId(offer));
        builder.asset(Asset.Builder.newInstance().id(policy.getTarget()).build());
        builder.policy(policy);

        return builder.build();
    }
}
