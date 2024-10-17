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
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_EVENT_TYPE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_ACCEPTED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_FINALIZED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;


/**
 * Creates a {@link JsonObject} from a {@link ContractNegotiationEventMessage}.
 */
public class JsonObjectFromContractNegotiationEventMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<ContractNegotiationEventMessage, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractNegotiationEventMessageTransformer(JsonBuilderFactory jsonFactory) {
        this(jsonFactory, DSPACE_SCHEMA);
    }

    public JsonObjectFromContractNegotiationEventMessageTransformer(JsonBuilderFactory jsonFactory, String namespace) {
        super(ContractNegotiationEventMessage.class, JsonObject.class, namespace);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractNegotiationEventMessage eventMessage, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(ID, eventMessage.getId())
                .add(TYPE, forNamespace(DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM))
                .add(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM), eventMessage.getConsumerPid())
                .add(forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM), eventMessage.getProviderPid())
                .add(forNamespace(DSPACE_PROPERTY_EVENT_TYPE_TERM), switch (eventMessage.getType()) {
                    case ACCEPTED -> forNamespace(DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_ACCEPTED_TERM);
                    case FINALIZED -> forNamespace(DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_FINALIZED_TERM);
                })
                .build();
    }

}
