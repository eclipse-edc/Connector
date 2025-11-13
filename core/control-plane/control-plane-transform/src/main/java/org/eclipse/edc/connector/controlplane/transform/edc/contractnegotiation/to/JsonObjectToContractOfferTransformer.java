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

package org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObjectToContractOfferTransformer extends AbstractJsonLdTransformer<JsonObject, ContractOffer> {

    public JsonObjectToContractOfferTransformer() {
        super(JsonObject.class, ContractOffer.class);
    }

    @Override
    public @Nullable ContractOffer transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var policy = context.transform(jsonObject, Policy.class);
        if (policy == null) {
            return null;
        }
        var id = nodeId(jsonObject);
        return ContractOffer.Builder.newInstance()
                .id(id)
                .assetId(policy.getTarget())
                .policy(policy)
                .build();
    }
}
