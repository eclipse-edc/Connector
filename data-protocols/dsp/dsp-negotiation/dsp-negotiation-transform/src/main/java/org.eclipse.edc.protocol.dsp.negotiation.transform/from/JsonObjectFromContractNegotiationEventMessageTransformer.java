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
import org.eclipse.edc.jsonld.JsonLdKeywords;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_EVENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CHECKSUM;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_ACCEPTED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_FINALIZED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;

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
    public @Nullable JsonObject transform(@NotNull ContractNegotiationEventMessage object, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(JsonLdKeywords.ID, String.valueOf(UUID.randomUUID()));
        builder.add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_EVENT_MESSAGE);

        builder.add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, object.getProcessId());
        builder.add(DSPACE_NEGOTIATION_PROPERTY_CHECKSUM, object.getChecksum());

        var eventType = eventType(object, context);
        if (eventType == null) {
            return null;
        }

        builder.add(DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE, eventType);


        return builder.build();
    }

    private String eventType(ContractNegotiationEventMessage message, TransformerContext context) {
        switch (message.getType()) {
            case ACCEPTED:
                return DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_ACCEPTED;
            case FINALIZED:
                return DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_FINALIZED;
            default:
                context.reportProblem(String.format("Could not map eventType %s", message.getType()));
                return null;
        }
    }

}
