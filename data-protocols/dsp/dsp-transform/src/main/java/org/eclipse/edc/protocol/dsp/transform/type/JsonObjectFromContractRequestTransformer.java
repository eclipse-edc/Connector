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

package org.eclipse.edc.protocol.dsp.transform.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.jsonld.transformer.JsonLdKeywords;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static org.eclipse.edc.protocol.dsp.transform.DspNamespaces.DSPACE_SCHEMA;

/**
 * Creates a {@link JsonObject} from a {@link ContractOfferRequest}.
 */
public class JsonObjectFromContractRequestTransformer extends AbstractJsonLdTransformer<ContractOfferRequest, JsonObject> {

    private final JsonBuilderFactory jsonFactory;
    private final ObjectMapper mapper;

    public JsonObjectFromContractRequestTransformer(JsonBuilderFactory jsonFactory, ObjectMapper mapper) {
        super(ContractOfferRequest.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.mapper = mapper;
    }

    @Override
    public @Nullable JsonObject transform(@Nullable ContractOfferRequest message, @NotNull TransformerContext context) {
        if (message == null) {
            return null;
        }

        var builder = jsonFactory.createObjectBuilder();
        builder.add(JsonLdKeywords.ID, String.valueOf(UUID.randomUUID()));
        builder.add(JsonLdKeywords.TYPE, DSPACE_SCHEMA + "ContractRequestMessage");

        builder.add(DSPACE_SCHEMA + "dataSet", message.getContractOffer().getAsset().getId());
        builder.add(DSPACE_SCHEMA + "processId", message.getCorrelationId());
        builder.add(DSPACE_SCHEMA + "offer", transformContractOffer(message.getContractOffer(), context));

        return builder.build();
    }

    private @Nullable JsonObject transformContractOffer(ContractOffer offer, TransformerContext context) {
        return context.transform(offer, JsonObject.class);
    }

}
