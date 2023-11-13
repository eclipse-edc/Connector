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

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.ASSET_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.OFFER_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.POLICY;

@Deprecated(since = "0.3.2")
public class JsonObjectToContractOfferDescriptionTransformer extends AbstractJsonLdTransformer<JsonObject, ContractOfferDescription> {

    public JsonObjectToContractOfferDescriptionTransformer() {
        super(JsonObject.class, ContractOfferDescription.class);
    }

    @Override
    public @Nullable ContractOfferDescription transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = ContractOfferDescription.Builder.newInstance();

        visitProperties(jsonObject, (k, v) -> setProperties(k, v, builder, context));
        return builder.build();
    }

    private void setProperties(String key, JsonValue value, ContractOfferDescription.Builder builder, TransformerContext context) {
        switch (key) {
            case OFFER_ID:
                transformString(value, builder::offerId, context);
                break;
            case ASSET_ID:
                transformString(value, builder::assetId, context);
                break;
            case POLICY:
                transformArrayOrObject(value, Policy.class, builder::policy, context);
                break;
            default:
                context.reportProblem("Cannot convert key " + key + " as it is not known");
                break;
        }
    }
}
