/*
 *  Copyright (c) 2023 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Negotiation API enhancement
 *
 */

package org.eclipse.edc.connector.api.management.contractnegotiation.v3.transform;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.management.contractnegotiation.v3.model.ContractRequestDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static org.eclipse.edc.connector.api.management.contractnegotiation.v3.model.ContractRequestDto.ASSET_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.v3.model.ContractRequestDto.CALLBACK_ADDRESSES;
import static org.eclipse.edc.connector.api.management.contractnegotiation.v3.model.ContractRequestDto.CONNECTOR_ADDRESS;
import static org.eclipse.edc.connector.api.management.contractnegotiation.v3.model.ContractRequestDto.POLICY;
import static org.eclipse.edc.connector.api.management.contractnegotiation.v3.model.ContractRequestDto.PROTOCOL;
import static org.eclipse.edc.connector.api.management.contractnegotiation.v3.model.ContractRequestDto.PROVIDER_ID;

public class JsonObjectToContractRequestDtoTransformer extends AbstractJsonLdTransformer<JsonObject, ContractRequestDto> {

    public JsonObjectToContractRequestDtoTransformer() {
        super(JsonObject.class, ContractRequestDto.class);
    }

    @Override
    public @Nullable ContractRequestDto transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var contractRequestDtoBuilder = ContractRequestDto.Builder.newInstance()
                .providerId(getProviderId(jsonObject, context))
                .counterPartyAddress(transformString(jsonObject.get(CONNECTOR_ADDRESS), context))
                .protocol(transformString(jsonObject.get(PROTOCOL), context))
                .assetId(transformString(jsonObject.get(ASSET_ID), context));

        var contractPolicy = transformObject(jsonObject.get(POLICY), Policy.class, context);
        var policy = jsonObject.get(POLICY);
        var policyId = nodeId(policy);
        contractRequestDtoBuilder.policyId(policyId);
        contractRequestDtoBuilder.policy(contractPolicy);

        var callbackAddress = jsonObject.get(CALLBACK_ADDRESSES);
        if (callbackAddress != null) {
            var addresses = new ArrayList<CallbackAddress>();
            transformArrayOrObject(callbackAddress, CallbackAddress.class, addresses::add, context);
            contractRequestDtoBuilder.callbackAddresses(addresses);
        }

        return contractRequestDtoBuilder.build();
    }

    private String getProviderId(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var providerId = jsonObject.get(PROVIDER_ID);
        if (providerId != null) {
            return transformString(providerId, context);
        }

        return transformString(jsonObject.get(CONNECTOR_ADDRESS), context);

    }

}
