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
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage.Type.ACCEPTED;
import static org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage.Type.FINALIZED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CHECKSUM;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_ACCEPTED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_FINALIZED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;

/**
 * Creates a {@link ContractNegotiationEventMessage} from a {@link JsonObject}.
 */
public class JsonObjectToContractNegotiationEventMessageTransformer extends AbstractJsonLdTransformer<JsonObject, ContractNegotiationEventMessage> {

    private TransformerContext context;
    private ContractNegotiationEventMessage.Type eventType;

    public JsonObjectToContractNegotiationEventMessageTransformer() {
        super(JsonObject.class, ContractNegotiationEventMessage.class);
    }

    @Override
    public @Nullable ContractNegotiationEventMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        this.context = context;

        var builder = ContractNegotiationEventMessage.Builder.newInstance();
        builder.protocol(DATASPACE_PROTOCOL_HTTP);
        transformString(object.get(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID), builder::processId, context);
        transformString(object.get(DSPACE_NEGOTIATION_PROPERTY_CHECKSUM), builder::checksum, context);

        transformString(object.get(DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE), this::type, context);

        if (eventType == null) {
            return null;
        }

        builder.type(eventType);

        return builder.build();
    }

    private void type(String type) {
        switch (type) {
            case DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_ACCEPTED:
                eventType = ACCEPTED;
                break;
            case DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_FINALIZED:
                eventType = FINALIZED;
                break;
            default:
                context.reportProblem(String.format("Could not map type %s", type));
        }
    }

}
