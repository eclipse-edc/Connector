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

package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToContractOfferTransformer extends AbstractJsonLdTransformer<JsonObject, ContractOffer> {

    public ToContractOfferTransformer() {
        super(JsonObject.class, ContractOffer.class);
    }

    @Override
    public @Nullable ContractOffer transform(@Nullable JsonObject object, @NotNull TransformerContext context) {
        return null;
    }
}
