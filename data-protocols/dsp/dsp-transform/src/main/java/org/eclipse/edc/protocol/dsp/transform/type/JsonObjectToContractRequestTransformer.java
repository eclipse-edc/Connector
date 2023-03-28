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
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Creates a {@link ContractOfferRequest} from a {@link JsonObject}.
 */
public class JsonObjectToContractRequestTransformer extends AbstractJsonLdTransformer<JsonObject, ContractOfferRequest> {

    public JsonObjectToContractRequestTransformer() {
        super(JsonObject.class, ContractOfferRequest.class);
    }

    @Override
    public @Nullable ContractOfferRequest transform(@Nullable JsonObject input, @NotNull TransformerContext context) {
        if (input == null) {
            return null;
        }

        return ContractOfferRequest.Builder.newInstance().build();
    }

}
