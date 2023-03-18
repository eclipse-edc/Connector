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
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.jsonld.transformer.JsonLdKeywords;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.transform.DspNamespaces.DSPACE_SCHEMA;

/**
 * Creates a {@link JsonObject} from a {@link ContractNegotiation}.
 */
public class JsonObjectFromContractNegotiationTransformer extends AbstractJsonLdTransformer<ContractNegotiation, JsonObject> {

    private final JsonBuilderFactory jsonFactory;
    private final ObjectMapper mapper;

    public JsonObjectFromContractNegotiationTransformer(JsonBuilderFactory jsonFactory, ObjectMapper mapper) {
        super(ContractNegotiation.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.mapper = mapper;
    }

    @Override
    public @Nullable JsonObject transform(@Nullable ContractNegotiation negotiation, @NotNull TransformerContext context) {
        if (negotiation == null) {
            return null;
        }

        var builder = jsonFactory.createObjectBuilder();
        builder.add(JsonLdKeywords.ID, negotiation.getId());
        builder.add(JsonLdKeywords.TYPE, DSPACE_SCHEMA + "ContractNegotiation");

        builder.add(DSPACE_SCHEMA + "correlationId", negotiation.getCorrelationId());
        builder.add(DSPACE_SCHEMA + "state", ContractNegotiationStates.from(negotiation.getState()).name());
        //builder.add(DSPACE_PREFIX + "checksum", negotiation.getChecksum());

        return builder.build();
    }

}
