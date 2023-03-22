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

package org.eclipse.edc.jsonld.transformer.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.transformer.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.spi.types.domain.DataAddress.TYPE;

/**
 * Converts a {@link ContractOffer} to a DCAT Dataset as a {@link JsonObject} in JSON-LD expanded form.
 */
public class FromContractOfferTransformer extends AbstractJsonLdTransformer<ContractOffer, JsonObject> {
    private final JsonBuilderFactory jsonFactory;

    public FromContractOfferTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractOffer.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@Nullable ContractOffer contractOffer, @NotNull TransformerContext context) {
        if (contractOffer == null) {
            return null;
        }

        var datasetBuilder = jsonFactory.createObjectBuilder();
        datasetBuilder.add(ID, contractOffer.getId());
        datasetBuilder.add(TYPE, DCAT_SCHEMA + "Datset");

        transformPolicy(contractOffer, datasetBuilder, context);

        addAssetData(contractOffer.getAsset(), datasetBuilder, context);

        return datasetBuilder.build();
    }

    private void addAssetData(Asset asset, JsonObjectBuilder datasetBuilder, TransformerContext context) {
    }

    private void transformPolicy(ContractOffer contractOffer, JsonObjectBuilder datasetBuilder, TransformerContext context) {
        var policy = contractOffer.getPolicy();
        var policyObject = context.transform(policy, JsonObject.class);
    }
}
