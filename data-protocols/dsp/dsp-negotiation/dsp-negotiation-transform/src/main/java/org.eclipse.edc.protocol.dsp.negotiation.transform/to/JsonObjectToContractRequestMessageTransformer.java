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
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage.Type.INITIAL;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_DATASET;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
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
                .type(INITIAL)
                .processId(transformString(object.get(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID), context))
                .callbackAddress(transformString(object.get(DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS), context))
                .dataSet(transformString(object.get(DSPACE_NEGOTIATION_PROPERTY_DATASET), context));

        var policy = transformObject(object.get(DSPACE_NEGOTIATION_PROPERTY_OFFER), Policy.class, context);
        if (policy == null) {
            context.reportProblem("Cannot transform to ContractRequestMessage with null policy");
            return null;
        }

        var contractOffer = ContractOffer.Builder.newInstance()
                .id(nodeId(object))
                .assetId(policy.getTarget())
                .policy(policy).build();

        builder.contractOffer(contractOffer);

        return builder.build();
    }

}
