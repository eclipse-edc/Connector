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
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage.Type.ACCEPTED;
import static org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage.Type.FINALIZED;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_ACCEPTED;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_FINALIZED;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;

/**
 * Creates a {@link ContractNegotiationEventMessage} from a {@link JsonObject}.
 */
public class JsonObjectToContractNegotiationEventMessageTransformer extends AbstractJsonLdTransformer<JsonObject, ContractNegotiationEventMessage> {

    public JsonObjectToContractNegotiationEventMessageTransformer() {
        super(JsonObject.class, ContractNegotiationEventMessage.class);
    }

    @Override
    public @Nullable ContractNegotiationEventMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {

        var builder = ContractNegotiationEventMessage.Builder.newInstance();
        builder.protocol(DATASPACE_PROTOCOL_HTTP);

        if (!transformMandatoryString(object.get(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID), builder::processId, context)) {
            context.reportProblem(format("ContractNegotiationEventMessage is missing the %s property", DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID));
            return null;
        }

        var eventType = transformString(object.get(DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE), context);
        if (DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_ACCEPTED.equals(eventType)) {
            builder.type(ACCEPTED);
        } else if (DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_FINALIZED.equals(eventType)) {
            builder.type(FINALIZED);
        } else {
            context.reportProblem(format("Could not map '%s' in ContractNegotiationEventMessage: %s", DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE, eventType));
            return null;
        }

        return builder.build();
    }

}
