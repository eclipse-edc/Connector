/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.transform.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationError;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_TERM;

/**
 * Transforms a {@link ContractNegotiationError} to a {@link JsonObject} in JSON-LD expanded form.
 */
public class JsonObjectFromContractNegotiationErrorTransformer extends AbstractNamespaceAwareJsonLdTransformer<ContractNegotiationError, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractNegotiationErrorTransformer(JsonBuilderFactory jsonFactory) {
        this(jsonFactory, DSPACE_SCHEMA);
    }

    public JsonObjectFromContractNegotiationErrorTransformer(JsonBuilderFactory jsonFactory, String namespace) {
        super(ContractNegotiationError.class, JsonObject.class, namespace);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractNegotiationError error, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, forNamespace(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR_TERM))
                .add(forNamespace(DSPACE_PROPERTY_CODE_TERM), error.getCode())
                .add(forNamespace(DSPACE_PROPERTY_REASON_TERM), jsonFactory.createArrayBuilder(error.getMessages()))
                .build();
    }
}
