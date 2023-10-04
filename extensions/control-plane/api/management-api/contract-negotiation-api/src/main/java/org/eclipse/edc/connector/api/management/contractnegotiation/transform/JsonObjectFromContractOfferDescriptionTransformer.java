/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.contractnegotiation.transform;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.*;


public class JsonObjectFromContractOfferDescriptionTransformer extends AbstractJsonLdTransformer<ContractOfferDescription, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractOfferDescriptionTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractOfferDescription.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractOfferDescription contractOfferDescription, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();

        builder.add(OFFER_ID, contractOfferDescription.getOfferId())
                .add(ASSET_ID, contractOfferDescription.getAssetId());

        return builder.build();
    }

}
