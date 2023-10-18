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

import org.eclipse.edc.connector.api.management.contractnegotiation.v3.model.ContractRequestDto;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ContractRequestDtoToContractRequestTransformer implements TypeTransformer<ContractRequestDto, ContractRequest> {

    @Override
    public Class<ContractRequestDto> getInputType() {
        return ContractRequestDto.class;
    }

    @Override
    public Class<ContractRequest> getOutputType() {
        return ContractRequest.class;
    }

    @Override
    public @Nullable ContractRequest transform(@NotNull ContractRequestDto contractRequestDto, @NotNull TransformerContext context) {

        var contractRequestBuilder = ContractRequest.Builder.newInstance()
                .providerId(contractRequestDto.getProviderId())
                .counterPartyAddress(contractRequestDto.getCounterPartyAddress())
                .protocol(contractRequestDto.getProtocol());

        var contractOffer = ContractOffer.Builder.newInstance()
                .id(contractRequestDto.getPolicyId())
                .assetId(contractRequestDto.getAssetId())
                .policy(contractRequestDto.getPolicy())
                .build();
        contractRequestBuilder.contractOffer(contractOffer);

        contractRequestBuilder.callbackAddresses(contractRequestDto.getCallbackAddresses());
        return contractRequestBuilder.build();
    }
}
