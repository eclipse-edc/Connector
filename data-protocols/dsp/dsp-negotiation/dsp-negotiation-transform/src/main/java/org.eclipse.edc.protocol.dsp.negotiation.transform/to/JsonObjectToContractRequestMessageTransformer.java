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

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_DATASET;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_OFFER_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;

/**
 * Creates a {@link ContractRequestMessage} from a {@link JsonObject}.
 */
public class JsonObjectToContractRequestMessageTransformer extends AbstractJsonLdTransformer<JsonObject, ContractRequestMessage> {

    public JsonObjectToContractRequestMessageTransformer() {
        super(JsonObject.class, ContractRequestMessage.class);
    }

    @Override
    public @Nullable ContractRequestMessage transform(@NotNull JsonObject requestObject, @NotNull TransformerContext context) {
        var builder = ContractRequestMessage.Builder.newInstance();
        if (!transformMandatoryString(requestObject.get(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID), builder::processId, context)) {
            context.reportProblem(format("No '%s' specified on ContractRequestMessage", DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID));
            return null;
        }

        var callback = requestObject.get(DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS);
        if (callback != null) {
            builder.callbackAddress(transformString(callback, context));
        }

        var dataset = requestObject.get(DSPACE_NEGOTIATION_PROPERTY_DATASET);
        if (dataset != null) {
            builder.dataSet(transformString(dataset, context));
        }

        var contractOffer = returnJsonObject(requestObject.get(DSPACE_NEGOTIATION_PROPERTY_OFFER), context, DSPACE_NEGOTIATION_PROPERTY_OFFER, false);
        if (contractOffer != null) {
            var policy = transformObject(contractOffer, Policy.class, context);
            if (policy == null) {
                context.reportProblem("Cannot transform to ContractRequestMessage with null policy");
                return null;
            }
            var id = nodeId(contractOffer);
            if (id == null) {
                context.reportProblem(format("@id must be specified when including a '%s' in a ContractRequestMessage", DSPACE_NEGOTIATION_PROPERTY_OFFER));
                return null;
            }
            var offer = ContractOffer.Builder.newInstance().id(id).assetId(policy.getTarget()).policy(policy).build();
            builder.contractOffer(offer);
        } else if (context.hasProblems()) {
            return null;
        } else {
            if (!transformMandatoryString(requestObject.get(DSPACE_NEGOTIATION_PROPERTY_OFFER_ID), builder::contractOfferId, context)) {
                context.reportProblem("ContractRequestMessage must specify a contract offer or contract offer id");
                return null;
            }
        }
        return builder.build();
    }

}


