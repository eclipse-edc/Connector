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

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Creates a {@link ContractNegotiation} from a {@link JsonObject}.
 */
public class JsonObjectToContractNegotiationTransformer extends AbstractJsonLdTransformer<JsonObject, ContractNegotiation> {

    public JsonObjectToContractNegotiationTransformer() {
        super(JsonObject.class, ContractNegotiation.class);
    }

    @Override
    public @Nullable ContractNegotiation transform(@Nullable JsonObject input, @NotNull TransformerContext context) {
        if (input == null) {
            return null;
        }

        return ContractNegotiation.Builder.newInstance().build();
    }

}
