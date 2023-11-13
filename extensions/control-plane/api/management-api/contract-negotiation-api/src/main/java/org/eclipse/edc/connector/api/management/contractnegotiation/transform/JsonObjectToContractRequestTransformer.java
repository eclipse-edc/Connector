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
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.spi.types.domain.offer.ContractOffer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static java.lang.String.format;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.CALLBACK_ADDRESSES;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.CONNECTOR_ADDRESS;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_TYPE;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.OFFER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.POLICY;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.PROTOCOL;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.PROVIDER_ID;

public class JsonObjectToContractRequestTransformer extends AbstractJsonLdTransformer<JsonObject, ContractRequest> {

    private final Monitor monitor;

    public JsonObjectToContractRequestTransformer(Monitor monitor) {
        super(JsonObject.class, ContractRequest.class);
        this.monitor = monitor;
    }

    @Override
    public @Nullable ContractRequest transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var contractRequestBuilder = ContractRequest.Builder.newInstance()
                .providerId(getProviderId(jsonObject, context))
                .counterPartyAddress(transformString(jsonObject.get(CONNECTOR_ADDRESS), context))
                .protocol(transformString(jsonObject.get(PROTOCOL), context));

        var policy = transformObject(jsonObject.get(POLICY), Policy.class, context);
        if (policy != null) {
            contractRequestBuilder.policy(policy);
        }

        var contractOfferDescription = transformObject(jsonObject.get(OFFER), ContractOfferDescription.class, context);
        if (contractOfferDescription != null) {
            monitor.warning(format("The attribute %s has been deprecated in type %s, please use %s",
                    OFFER, CONTRACT_REQUEST_TYPE, POLICY));
            var contractOffer = ContractOffer.Builder.newInstance()
                    .id(contractOfferDescription.getOfferId())
                    .assetId(contractOfferDescription.getAssetId())
                    .policy(contractOfferDescription.getPolicy())
                    .build();
            contractRequestBuilder.contractOffer(contractOffer);
        }

        var callbackAddress = jsonObject.get(CALLBACK_ADDRESSES);
        if (callbackAddress != null) {
            var addresses = new ArrayList<CallbackAddress>();
            transformArrayOrObject(callbackAddress, CallbackAddress.class, addresses::add, context);
            contractRequestBuilder.callbackAddresses(addresses);
        }

        return contractRequestBuilder.build();
    }

    private String getProviderId(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var providerId = jsonObject.get(PROVIDER_ID);
        if (providerId != null) {
            return transformString(providerId, context);
        }

        return transformString(jsonObject.get(CONNECTOR_ADDRESS), context);

    }

}
