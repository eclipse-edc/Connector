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
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.spi.types.domain.offer.ContractOffer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.CALLBACK_ADDRESSES;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.CONNECTOR_ADDRESS;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.OFFER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.POLICY;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.PROTOCOL;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest.PROVIDER_ID;

public class JsonObjectToContractRequestTransformer extends AbstractJsonLdTransformer<JsonObject, ContractRequest> {

    public JsonObjectToContractRequestTransformer() {
        super(JsonObject.class, ContractRequest.class);
    }

    @Override
    public @Nullable ContractRequest transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var contractRequestBuilder = ContractRequest.Builder.newInstance()
                .providerId(getProviderId(jsonObject, context))
                .counterPartyAddress(counterPartyAddressOrConnectorAddress(jsonObject, context))
                .protocol(transformString(jsonObject.get(PROTOCOL), context));

        contractRequestBuilder.contractOffer(contractOffer(jsonObject, context));

        var callbackAddress = jsonObject.get(CALLBACK_ADDRESSES);
        if (callbackAddress != null) {
            contractRequestBuilder.callbackAddresses(transformArray(callbackAddress, CallbackAddress.class, context));
        }

        return contractRequestBuilder.build();
    }

    private ContractOffer contractOffer(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var policy = jsonObject.get(POLICY);
        if (policy != null) {
            return transformObject(policy, ContractOffer.class, context);
        }

        var offerJson = jsonObject.get(OFFER);
        if (offerJson != null) {
            var contractOfferDescription = transformObject(jsonObject.get(OFFER), ContractOfferDescription.class, context);
            return ContractOffer.Builder.newInstance()
                    .id(contractOfferDescription.getOfferId())
                    .assetId(contractOfferDescription.getAssetId())
                    .policy(contractOfferDescription.getPolicy())
                    .build();
        }

        return null;
    }

    private String getProviderId(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var providerId = jsonObject.get(PROVIDER_ID);
        if (providerId != null) {
            return transformString(providerId, context);
        }

        return counterPartyAddressOrConnectorAddress(jsonObject, context);

    }

    /**
    * This method can be removed once `connectorAddress` is deleted and exists only for legacy reasons
    */
    private String counterPartyAddressOrConnectorAddress(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var counterPartyAddress = transformString(jsonObject.get(CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS), context);
        return counterPartyAddress != null ? counterPartyAddress : transformString(jsonObject.get(CONNECTOR_ADDRESS), context);
    }
}
