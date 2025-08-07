/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_OFFER_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;


/**
 * Creates a {@link JsonObject} from a {@link ContractRequestMessage}.
 */
public class JsonObjectFromContractRequestMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<ContractRequestMessage, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractRequestMessageTransformer(JsonBuilderFactory jsonFactory, JsonLdNamespace namespace) {
        super(ContractRequestMessage.class, JsonObject.class, namespace);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractRequestMessage requestMessage, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(ID, requestMessage.getId())
                .add(TYPE, forNamespace(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM))
                .add(forNamespace(DSPACE_PROPERTY_CONSUMER_PID_TERM), requestMessage.getConsumerPid());

        addIfNotNull(requestMessage.getProviderPid(), forNamespace(DSPACE_PROPERTY_PROVIDER_PID_TERM), builder);
        addIfNotNull(requestMessage.getCallbackAddress(), forNamespace(DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM), builder);

        var policy = context.transform(requestMessage.getContractOffer().getPolicy(), JsonObject.class);
        if (policy == null) {
            context.problem()
                    .nullProperty()
                    .type(ContractRequestMessage.class)
                    .property("contractOffer")
                    .report();
            return null;
        }

        var enrichedPolicy = Json.createObjectBuilder(policy)
                .add(ID, requestMessage.getContractOffer().getId())
                .build();
        builder.add(forNamespace(DSPACE_PROPERTY_OFFER_TERM), enrichedPolicy);

        return builder.build();
    }

}
