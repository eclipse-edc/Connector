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
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_EVENT_TYPE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_ACCEPTED;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_FINALIZED;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROCESS_ID;


/**
 * Creates a {@link JsonObject} from a {@link ContractNegotiationEventMessage}.
 */
public class JsonObjectFromContractNegotiationEventMessageTransformer extends AbstractJsonLdTransformer<ContractNegotiationEventMessage, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractNegotiationEventMessageTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractNegotiationEventMessage.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractNegotiationEventMessage eventMessage, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(ID, eventMessage.getId());
        builder.add(TYPE, DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE);

        builder.add(DSPACE_PROPERTY_PROCESS_ID, eventMessage.getProcessId());

        builder.add(DSPACE_PROPERTY_EVENT_TYPE, eventType(eventMessage));

        return builder.build();
    }

    @NotNull
    private String eventType(ContractNegotiationEventMessage message) {
        switch (message.getType()) {
            case ACCEPTED:
                return DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_ACCEPTED;
            case FINALIZED:
                return DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_FINALIZED;
            default:
                // this cannot happen
                throw new EdcException("Unknown event type: " + message.getType());
        }
    }

}
