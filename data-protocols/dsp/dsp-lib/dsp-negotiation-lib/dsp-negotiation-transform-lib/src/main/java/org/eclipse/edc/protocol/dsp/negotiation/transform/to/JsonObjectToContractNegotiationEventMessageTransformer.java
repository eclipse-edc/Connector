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
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage.Type.ACCEPTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage.Type.FINALIZED;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_EVENT_TYPE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_ACCEPTED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_FINALIZED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;

/**
 * Creates a {@link ContractNegotiationEventMessage} from a {@link JsonObject}.
 */
public class JsonObjectToContractNegotiationEventMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<JsonObject, ContractNegotiationEventMessage> {

    public JsonObjectToContractNegotiationEventMessageTransformer(JsonLdNamespace namespace) {
        super(JsonObject.class, ContractNegotiationEventMessage.class, namespace);
    }

    @Override
    public @Nullable ContractNegotiationEventMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = ContractNegotiationEventMessage.Builder.newInstance();

        if (!transformMandatoryString(object.get(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM)), builder::consumerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM))
                    .report();
            return null;
        }
        if (!transformMandatoryString(object.get(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM)), builder::providerPid, context)) {
            context.problem()
                    .missingProperty()
                    .type(forNamespace(DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM))
                    .report();
            return null;
        }

        var eventType = transformString(object.get(forNamespace(DSPACE_PROPERTY_EVENT_TYPE_TERM)), context);
        if (forNamespace(DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_ACCEPTED_TERM).equals(eventType)) {
            builder.type(ACCEPTED);
        } else if (forNamespace(DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_FINALIZED_TERM).equals(eventType)) {
            builder.type(FINALIZED);
        } else {
            context.problem()
                    .unexpectedType()
                    .type(forNamespace(DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM))
                    .property(forNamespace(DSPACE_PROPERTY_EVENT_TYPE_TERM))
                    .expected(forNamespace(DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_ACCEPTED_TERM))
                    .expected(forNamespace(DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_FINALIZED_TERM))
                    .report();
            return null;
        }

        return builder.build();
    }

}
