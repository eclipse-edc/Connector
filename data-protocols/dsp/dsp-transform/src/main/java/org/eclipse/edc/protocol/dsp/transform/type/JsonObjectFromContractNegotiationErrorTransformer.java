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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.jsonld.transformer.JsonLdKeywords;
import org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationError;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static org.eclipse.edc.protocol.dsp.transform.DspNamespaces.DSPACE_SCHEMA;

/**
 * Creates a {@link JsonObject} from a {@link ContractNegotiationError}.
 */
public class JsonObjectFromContractNegotiationErrorTransformer extends AbstractJsonLdTransformer<ContractNegotiationError, JsonObject> {

    private final JsonBuilderFactory jsonFactory;
    private final ObjectMapper mapper;

    public JsonObjectFromContractNegotiationErrorTransformer(JsonBuilderFactory jsonFactory, ObjectMapper mapper) {
        super(ContractNegotiationError.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.mapper = mapper;
    }

    @Override
    public @Nullable JsonObject transform(@Nullable ContractNegotiationError error, @NotNull TransformerContext context) {
        if (error == null) {
            return null;
        }

        var builder = jsonFactory.createObjectBuilder();
        builder.add(JsonLdKeywords.ID, String.valueOf(UUID.randomUUID()));
        builder.add(JsonLdKeywords.TYPE, DSPACE_SCHEMA + "ContractNegotiationError");

        var reasons = error.getReasons().stream()
                .collect(jsonFactory::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();

        builder.add(DSPACE_SCHEMA + "processId", error.getProcessId());
        builder.add(DSPACE_SCHEMA + "code", error.getCode());
        builder.add(DSPACE_SCHEMA + "reason", reasons);

        return builder.build();
    }

}
